"""Recorrido funcional de las historias de usuario contra la pila de Docker.

Entra por `edge` (puerto 8080) con tokens reales de Keycloak (puerto 8180) y recorre HU-01 a HU-06: crear con
validación estructural, enviar, asignar de 1 a 3 revisores, evaluar por unanimidad, rechazar, corregir y reenviar,
notificaciones de los implicados, historial y auditoría, y además los simulacros (RF-21 a RF-24) y el seguimiento
académico (RF-25 a RF-28) con un estudiante y un docente. No necesita librerías externas.

    docker compose up -d --build          # desde microservicios/
    python pruebas-funcionales/recorrido_funcional.py

Variables opcionales: BASE (por defecto http://localhost:8080), KC (http://localhost:8180) y PW (la clave de
desarrollo de los usuarios del realm, que se lee de infraestructura/keycloak/bancopreguntas-realm.json). Con
PRUEBA_TIEMPO=1 se comprueba además el cierre por tiempo agotado, que tarda unos 100 segundos.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

BASE = os.environ.get('BASE', 'http://localhost:8080')
KC = os.environ.get('KC', 'http://localhost:8180') + '/realms/bancopreguntas/protocol/openid-connect/token'
API = BASE + '/api'
REALM = Path(__file__).resolve().parent.parent / 'infraestructura' / 'keycloak' / 'bancopreguntas-realm.json'
RESULTADOS = []


def clave_de_desarrollo():
    if os.environ.get('PW'):
        return os.environ['PW']
    usuarios = json.loads(REALM.read_text(encoding='utf-8'))['users']
    return next(c['value'] for u in usuarios if u['username'] == 'autor1' for c in u['credentials'])


def http(metodo, url, token=None, cuerpo=None, form=None):
    datos, cab = None, {}
    if token:
        cab['Authorization'] = 'Bearer ' + token
    if cuerpo is not None:
        datos = json.dumps(cuerpo).encode()
        cab['Content-Type'] = 'application/json'
    if form is not None:
        datos = urllib.parse.urlencode(form).encode()
        cab['Content-Type'] = 'application/x-www-form-urlencoded'
    peticion = urllib.request.Request(url, data=datos, headers=cab, method=metodo)
    try:
        with urllib.request.urlopen(peticion, timeout=30) as r:
            texto = r.read().decode()
            return r.status, (json.loads(texto) if texto.strip().startswith(('{', '[')) else texto)
    except urllib.error.HTTPError as e:
        texto = e.read().decode()
        try:
            return e.code, json.loads(texto)
        except ValueError:
            return e.code, texto


def token(usuario, clave):
    estado, j = http('POST', KC, form={'grant_type': 'password', 'client_id': 'bancopreguntas-web',
                                       'username': usuario, 'password': clave})
    assert estado == 200, (usuario, estado, j)
    return j['access_token']


def chequeo(nombre, condicion, detalle=''):
    RESULTADOS.append((nombre, bool(condicion)))
    print('OK   ' if condicion else 'FALLA', nombre, ('— ' + str(detalle)) if (detalle and not condicion) else '')


def esperar(condicion, intentos=20, pausa=0.5):
    """Los eventos viajan por RabbitMQ: se espera a que el efecto aparezca en vez de dormir un tiempo fijo."""
    for _ in range(intentos):
        if condicion():
            return True
        time.sleep(pausa)
    return condicion()


PREGUNTA = {
    'nombre': 'Idea principal de un texto',
    'contexto': 'La energía solar se ha vuelto una alternativa limpia y cada vez más barata frente a los combustibles fósiles.',
    'enunciado': '¿Cuál es la idea principal del texto?',
    'opcionA': 'La energía solar es costosa',
    'opcionB': 'La energía solar es una alternativa limpia y económica',
    'opcionC': 'Los combustibles fósiles son renovables',
    'opcionD': 'La energía solar solo sirve de día',
    'respuestaCorrecta': 'B',
    'justificacion': 'El texto presenta la energía solar como alternativa limpia y barata.',
    'bibliografia': 'Texto de ejemplo, 2026',
    'competencia': 'LECTURA_CRITICA',
    'tema': 'Idea principal',
    'subtema': 'Textos argumentativos',
    'dificultad': 'BASICO',
}


def main():
    sys.stdout.reconfigure(encoding='utf-8')
    clave = clave_de_desarrollo()
    t = {u: token(u, clave) for u in ['autor1', 'admin', 'revisor1', 'revisor2', 'revisor3', 'revisor4',
                                      'estudiante1', 'estudiante2', 'docente1']}
    ids = {u: http('GET', API + '/users/me', t[u])[1].get('id') for u in t}
    chequeo('Login con Keycloak y /users/me devuelve el usuario', all(ids.values()), ids)
    estado, _ = http('GET', API + '/questions')
    chequeo('Sin token responde 401', estado == 401, estado)
    estado, _ = http('GET', API + '/reviews/pending-assignment', t['autor1'])
    chequeo('Un autor no puede usar endpoints de administrador (403)', estado == 403, estado)

    # HU-01
    estado, q1 = http('POST', API + '/questions', t['autor1'], PREGUNTA)
    chequeo('HU-01 crear una pregunta válida: 201 y estado Borrador', estado == 201 and q1.get('estado') == 'BORRADOR', (estado, q1))
    q1id = q1.get('id')
    for nombre, cuerpo in [('dos preguntas directas', {**PREGUNTA, 'enunciado': '¿Qué es? ¿Y esto?'}),
                           ('opción «todas las anteriores»', {**PREGUNTA, 'opcionD': 'Todas las anteriores'}),
                           ('falta de contexto', {**PREGUNTA, 'contexto': ' '}),
                           ('opciones repetidas', {**PREGUNTA, 'opcionA': PREGUNTA['opcionB']})]:
        estado, j = http('POST', API + '/questions', t['autor1'], cuerpo)
        chequeo('HU-01 la validación estructural rechaza ' + nombre, estado in (400, 422), (estado, j))

    # HU-03
    estado, lista = http('GET', API + '/questions?estado=BORRADOR&page=0&size=5', t['autor1'])
    chequeo('HU-03 listar mis preguntas con filtro y paginación', estado == 200 and 'contenido' in lista, (estado, str(lista)[:200]))

    # HU-02
    estado, j = http('POST', API + '/questions/%s/submit' % q1id, t['autor1'])
    chequeo('HU-02 enviar a revisión: Pendiente de revisión', estado == 200 and j.get('estado') == 'PENDIENTE_REVISION', (estado, j))
    estado, j = http('POST', API + '/questions/%s/submit' % q1id, t['autor1'])
    chequeo('HU-02 no se puede enviar dos veces (transición inválida)', estado in (400, 409, 422), (estado, j))

    # HU-04
    chequeo('HU-04 la pregunta aparece pendiente de asignación', esperar(
        lambda: q1id in json.dumps(http('GET', API + '/reviews/pending-assignment', t['admin'])[1])))
    asignar = API + '/reviews/%s/assignments' % q1id
    estado, j = http('POST', asignar, t['admin'], {'reviewerIds': []})
    chequeo('HU-04 con 0 revisores se rechaza', estado in (400, 422), (estado, j))
    estado, j = http('POST', asignar, t['admin'], {'reviewerIds': [ids['revisor1'], ids['revisor2'], ids['revisor3'], ids['revisor4']]})
    chequeo('HU-04 con 4 revisores se rechaza (el máximo es 3)', estado in (400, 422), (estado, j))
    estado, j = http('POST', asignar, t['admin'], {'reviewerIds': [ids['autor1']]})
    chequeo('HU-04 el autor no puede ser revisor', estado in (400, 403, 409, 422), (estado, j))
    estado, j = http('POST', asignar, t['admin'], {'reviewerIds': [ids['revisor1'], ids['revisor2']]})
    chequeo('HU-04 asignar 2 revisores', estado in (200, 201), (estado, j))
    chequeo('HU-04 la pregunta pasa a En revisión', esperar(
        lambda: http('GET', API + '/questions/%s' % q1id, t['autor1'])[1].get('estado') == 'EN_REVISION'))

    def tipos(usuario):
        estado, j = http('GET', API + '/notifications?size=50', t[usuario])
        return [n['tipo'] for n in j.get('contenido', [])] if estado == 200 else []

    chequeo('HU-04 el revisor recibe la notificación de su asignación', esperar(lambda: 'REVISORES_ASIGNADOS' in tipos('revisor1')))
    chequeo('HU-04 el autor recibe el aviso de que su pregunta entró en revisión', esperar(lambda: 'PREGUNTA_EN_REVISION' in tipos('autor1')))

    # HU-05
    estado, mias = http('GET', API + '/reviews/assigned', t['revisor1'])
    chequeo('HU-05 el revisor lista las preguntas que le asignaron', estado == 200 and q1id in json.dumps(mias), (estado, str(mias)[:200]))
    evaluar = API + '/reviews/%s/evaluations' % q1id
    estado, j = http('POST', evaluar, t['revisor1'], {'decision': 'RECHAZAR'})
    chequeo('HU-05 rechazar exige observación', estado in (400, 422), (estado, j))
    estado, j = http('POST', evaluar, t['revisor1'], {'decision': 'APROBAR', 'observacion': 'Cumple con los criterios'})
    chequeo('HU-05 el revisor 1 aprueba', estado in (200, 201), (estado, j))
    chequeo('HU-05 el administrador recibe cada evaluación', esperar(lambda: 'EVALUACION_RECIBIDA' in tipos('admin')))
    chequeo('HU-05 con una sola aprobación sigue En revisión (unanimidad)',
            http('GET', API + '/questions/%s' % q1id, t['autor1'])[1].get('estado') == 'EN_REVISION')
    estado, j = http('POST', evaluar, t['revisor2'], {'decision': 'APROBAR', 'observacion': 'De acuerdo'})
    chequeo('HU-05 el revisor 2 aprueba', estado in (200, 201), (estado, j))
    chequeo('HU-05 por unanimidad la pregunta queda Aprobada', esperar(
        lambda: http('GET', API + '/questions/%s' % q1id, t['autor1'])[1].get('estado') == 'APROBADA'))
    estado, rv = http('GET', API + '/reviews/%s' % q1id, t['autor1'])
    chequeo('HU-05 el autor lee las observaciones de los revisores', estado == 200 and 'Cumple con los criterios' in json.dumps(rv, ensure_ascii=False))
    chequeo('HU-05 el autor recibe el resultado', esperar(lambda: 'REVISION_APROBADA' in tipos('autor1')))
    chequeo('HU-05 el administrador y los revisores reciben el resultado de la revisión',
            esperar(lambda: 'RESULTADO_DE_REVISION' in tipos('admin') and 'RESULTADO_DE_REVISION' in tipos('revisor1')))
    estado, correos = http('GET', API + '/notifications/emails', t['admin'])
    chequeo('El administrador consulta los correos (simulados) enviados', estado == 200 and len(json.dumps(correos)) > 50, (estado, str(correos)[:120]))

    estado, j = http('POST', API + '/questions/%s/publish' % q1id, t['admin'])
    chequeo('El administrador publica la pregunta aprobada', estado == 200 and j.get('estado') == 'PUBLICADA', (estado, j))
    chequeo('El autor recibe el aviso de publicación', esperar(lambda: 'PREGUNTA_PUBLICADA' in tipos('autor1')))

    # Simulacros (RF-21 a RF-24)
    resumen = lambda: http('GET', API + '/simulations/catalog/summary', t['estudiante1'])[1]
    chequeo('RF-21 la pregunta publicada llega al catálogo de simulacros', esperar(
        lambda: resumen().get('porCompetencia', {}).get('LECTURA_CRITICA', 0) >= 1))
    estado, sim = http('POST', API + '/simulations', t['estudiante1'],
                       {'competencias': ['LECTURA_CRITICA'], 'cantidad': 1, 'duracionMinutos': 20})
    chequeo('RF-21 y RF-22 el estudiante genera un simulacro aleatorio con preguntas publicadas',
            estado == 201 and sim.get('estado') == 'EN_CURSO' and len(sim.get('preguntas', [])) == 1, (estado, sim))
    simid = sim.get('id')
    chequeo('RF-23 el simulacro trae el tiempo que le queda', 0 < sim.get('segundosRestantes', 0) <= 1200)
    chequeo('RF-24 no se revela la respuesta correcta mientras está en curso', 'respuestaCorrecta' not in sim['preguntas'][0])
    estado, _ = http('POST', API + '/simulations', t['estudiante1'], {'cantidad': 50, 'duracionMinutos': 20})
    chequeo('Pedir más preguntas de las que hay publicadas se rechaza (409)', estado == 409, estado)
    estado, _ = http('POST', API + '/simulations', t['estudiante1'], {'cantidad': 51, 'duracionMinutos': 20})
    chequeo('Pedir más del máximo de preguntas de un simulacro se rechaza (400)', estado == 400, estado)
    estado, _ = http('POST', API + '/simulations', t['estudiante1'], {'cantidad': 0, 'duracionMinutos': 20})
    chequeo('Un simulacro sin preguntas se rechaza (400)', estado == 400, estado)
    estado, _ = http('POST', API + '/simulations', t['docente1'], {'cantidad': 1, 'duracionMinutos': 20})
    chequeo('Un docente no genera simulacros (403)', estado == 403, estado)
    estado, _ = http('GET', API + '/simulations/%s' % simid, t['estudiante2'])
    chequeo('Un estudiante no ve el simulacro de otro (403)', estado == 403, estado)
    estado, _ = http('GET', API + '/simulations/%s' % simid, t['docente1'])
    chequeo('El docente sí consulta el simulacro de un estudiante', estado == 200, estado)
    qsim = sim['preguntas'][0]['preguntaId']
    estado, j = http('POST', API + '/simulations/%s/answers' % simid, t['estudiante1'], {'preguntaId': qsim, 'opcion': 'E'})
    chequeo('Una opción fuera de A–D se rechaza (400)', estado == 400, estado)
    estado, j = http('POST', API + '/simulations/%s/answers' % simid, t['estudiante1'], {'preguntaId': qsim, 'opcion': 'B'})
    chequeo('El estudiante contesta la pregunta', estado == 200 and j.get('respondidas') == 1, (estado, j))
    estado, fin = http('POST', API + '/simulations/%s/finish' % simid, t['estudiante1'])
    chequeo('RF-24 al finalizar se califica automáticamente y se revelan las respuestas',
            estado == 200 and fin.get('estado') == 'FINALIZADO' and fin.get('resultado', {}).get('puntaje') == 100.0
            and fin['preguntas'][0].get('respuestaCorrecta') == 'B', (estado, str(fin)[:300]))
    estado, _ = http('POST', API + '/simulations/%s/finish' % simid, t['estudiante1'])
    chequeo('Finalizar dos veces se rechaza (409)', estado == 409, estado)
    estado, _ = http('POST', API + '/simulations/%s/answers' % simid, t['estudiante1'], {'preguntaId': qsim, 'opcion': 'A'})
    chequeo('Un simulacro terminado ya no se contesta (409)', estado == 409, estado)
    estado, lista = http('GET', API + '/simulations', t['estudiante1'])
    chequeo('El estudiante lista sus simulacros', estado == 200 and simid in json.dumps(lista))

    # Seguimiento académico (RF-25 a RF-28)
    stats = lambda: http('GET', API + '/tracking/me/stats', t['estudiante1'])[1]
    historial = lambda: http('GET', API + '/tracking/me/history', t['estudiante1'])
    chequeo('RF-25 el resultado llega al seguimiento y queda en el historial', esperar(lambda: simid in json.dumps(historial()[1])))
    estado, hist = historial()
    chequeo('RF-25 el historial del estudiante trae el simulacro con su puntaje',
            estado == 200 and any(h['simulacroId'] == simid and h['puntaje'] == 100.0 for h in hist['contenido']), (estado, str(hist)[:200]))
    chequeo('RF-26 estadísticas individuales: promedio y mejor puntaje',
            stats().get('puntajePromedio') == 100.0 and stats().get('mejorPuntaje') == 100.0, stats())
    estado, comp = http('GET', API + '/tracking/me/competencies', t['estudiante1'])
    chequeo('RF-27 fortalezas y debilidades por competencia', estado == 200 and 'fortalezas' in comp and 'debilidades' in comp, (estado, comp))
    estado, de = http('GET', API + '/tracking/students/estudiante1/stats', t['docente1'])
    chequeo('RF-28 el docente consulta las estadísticas de un estudiante', estado == 200 and de.get('simulacros', 0) >= 1, (estado, de))
    estado, grupo = http('GET', API + '/tracking/reports/group', t['docente1'])
    chequeo('RF-28 reporte del grupo para el docente', estado == 200 and 'estudiante1' in json.dumps(grupo), (estado, str(grupo)[:200]))
    peticion = urllib.request.Request(API + '/tracking/reports/group.csv', headers={'Authorization': 'Bearer ' + t['admin']})
    csv = urllib.request.urlopen(peticion, timeout=30).read().decode()
    chequeo('RF-28 el reporte del grupo se exporta en CSV', csv.startswith('estudiante,simulacros,promedio') and 'estudiante1' in csv)
    estado, _ = http('GET', API + '/tracking/reports/group', t['estudiante1'])
    chequeo('Un estudiante no ve el reporte del grupo (403)', estado == 403, estado)
    estado, _ = http('GET', API + '/tracking/students/estudiante1/stats', t['estudiante2'])
    chequeo('Un estudiante no ve el seguimiento de otro (403)', estado == 403, estado)
    estado, j = http('GET', API + '/users/me', t['estudiante1'])
    chequeo('El estudiante consulta su perfil con su rol', estado == 200 and 'STUDENT' in j.get('roles', []), (estado, j))

    if os.environ.get('PRUEBA_TIEMPO') == '1':
        estado, corto = http('POST', API + '/simulations', t['estudiante2'],
                             {'competencias': ['LECTURA_CRITICA'], 'cantidad': 1, 'duracionMinutos': 1})
        chequeo('RF-23 (tiempo) se genera un simulacro de un minuto', estado == 201, (estado, corto))
        print('     esperando a que se agote el tiempo (unos 100 segundos)...')
        chequeo('RF-23 (tiempo) al agotarse el tiempo se cierra solo y llega al seguimiento como expirado', esperar(
            lambda: any(h.get('expirado') for h in http('GET', API + '/tracking/me/history', t['estudiante2'])[1].get('contenido', [])),
            intentos=60, pausa=2))
        estado, tarde = http('POST', API + '/simulations/%s/answers' % corto['id'], t['estudiante2'],
                             {'preguntaId': corto['preguntas'][0]['preguntaId'], 'opcion': 'B'})
        chequeo('RF-23 (tiempo) ya no se aceptan respuestas (409)', estado == 409, (estado, tarde))
        estado, cerrado = http('GET', API + '/simulations/%s' % corto['id'], t['estudiante2'])
        chequeo('RF-23 (tiempo) el simulacro queda EXPIRADO y calificado', estado == 200 and cerrado.get('estado') == 'EXPIRADO'
                and cerrado.get('resultado', {}).get('sinResponder') == 1, (estado, str(cerrado)[:200]))

    # Rechazo, corrección y nueva ronda (HU-06)
    estado, q2 = http('POST', API + '/questions', t['autor1'], {**PREGUNTA, 'nombre': 'Segunda pregunta'})
    q2id = q2['id']
    http('POST', API + '/questions/%s/submit' % q2id, t['autor1'])
    esperar(lambda: q2id in json.dumps(http('GET', API + '/reviews/pending-assignment', t['admin'])[1]))
    http('POST', API + '/reviews/%s/assignments' % q2id, t['admin'], {'reviewerIds': [ids['revisor1'], ids['revisor2']]})
    esperar(lambda: http('GET', API + '/questions/%s' % q2id, t['autor1'])[1].get('estado') == 'EN_REVISION')
    http('POST', API + '/reviews/%s/evaluations' % q2id, t['revisor1'], {'decision': 'RECHAZAR', 'observacion': 'El enunciado es ambiguo'})
    chequeo('Un solo rechazo deja la pregunta Rechazada', esperar(
        lambda: http('GET', API + '/questions/%s' % q2id, t['autor1'])[1].get('estado') == 'RECHAZADA'))
    estado, rv = http('GET', API + '/reviews/%s' % q2id, t['autor1'])
    chequeo('HU-06 el autor ve la observación del rechazo', 'ambiguo' in json.dumps(rv, ensure_ascii=False))
    estado, j = http('POST', API + '/questions/%s/reopen' % q2id, t['autor1'])
    chequeo('HU-06 el autor la reabre: vuelve a Borrador (versión 2)', estado == 200 and j.get('estado') == 'BORRADOR' and j.get('version') == 2, (estado, j))
    estado, j = http('POST', API + '/questions/%s/submit' % q2id, t['autor1'])
    chequeo('HU-06 la corregida se reenvía a revisión', estado == 200 and j.get('estado') == 'PENDIENTE_REVISION', (estado, j))
    chequeo('HU-06 se abre una ronda nueva con los mismos revisores (asignación automática)', esperar(
        lambda: http('GET', API + '/questions/%s' % q2id, t['autor1'])[1].get('estado') == 'EN_REVISION'))

    # Historial y auditoría
    chequeo('Historial de la pregunta visible para su autor', esperar(
        lambda: (lambda r: r[0] == 200 and len(json.dumps(r[1])) > 50)(http('GET', API + '/history/questions/%s' % q1id, t['autor1']))))
    estado, auditoria = http('GET', API + '/history/audit', t['admin'])
    chequeo('Auditoría de accesos (administrador)', estado == 200, (estado, str(auditoria)[:120]))
    estado, _ = http('GET', API + '/history/audit', t['autor1'])
    chequeo('La auditoría no es visible para un autor (403)', estado == 403, estado)

    # Swagger
    for nombre, url in [('Swagger UI del gateway', BASE + '/swagger-ui.html'),
                        ('OpenAPI de question-service', BASE + '/docs/question-service/v3/api-docs'),
                        ('OpenAPI de simulation-service', BASE + '/docs/simulation-service/v3/api-docs'),
                        ('OpenAPI de tracking-service', BASE + '/docs/tracking-service/v3/api-docs')]:
        try:
            chequeo(nombre, urllib.request.urlopen(url, timeout=15).status == 200)
        except urllib.error.HTTPError as e:
            chequeo(nombre, e.code == 200, e.code)

    fallos = [r for r in RESULTADOS if not r[1]]
    print('\nTotal: %d comprobaciones, %d fallos' % (len(RESULTADOS), len(fallos)))
    return 1 if fallos else 0


if __name__ == '__main__':
    sys.exit(main())
