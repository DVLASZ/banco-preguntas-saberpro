// Prueba de carga del sistema completo (k6): usuarios virtuales de los tres roles que consultan y escriben a
// través del gateway, con tokens reales de Keycloak. Se ejecuta con Docker, sin instalar nada:
//
//   docker run --rm -i -e PW=<clave de los usuarios de prueba> -e VUS=600 \
//     -e BASE=http://host.docker.internal:8080 -e KC=http://host.docker.internal:8180 \
//     grafana/k6 run - < pruebas-de-carga/escalabilidad.js
//
// VUS es el número máximo de usuarios simultáneos (por defecto 600). HOLD, cuánto se mantiene (por defecto 2m).
// PAUSA es el tiempo de lectura entre acciones en segundos (por defecto 1).
// Los tokens se piden una vez en setup() y se reparten entre los usuarios virtuales: así la prueba mide a los
// servicios y no a la pantalla de inicio de sesión de Keycloak.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const KC = __ENV.KC || 'http://localhost:8180';
const PW = __ENV.PW;
const VUS = parseInt(__ENV.VUS || '600', 10);
const HOLD = __ENV.HOLD || '2m';
// PAUSA: segundos base de lectura entre acciones (cada pausa dura entre PAUSA y 3×PAUSA). 1 = ritmo humano;
// valores como 0.05 convierten la prueba en una de estrés para buscar el punto de saturación.
const PAUSA = parseFloat(__ENV.PAUSA || '1');

const AUTORES = ['autor1', 'autor2'];
const REVISORES = ['revisor1', 'revisor2', 'revisor3', 'revisor4', 'revisor5'];

function etapas(fraccion) {
  const meta = Math.max(1, Math.round(VUS * fraccion));
  return [
    { duration: '1m', target: meta },
    { duration: HOLD, target: meta },
    { duration: '20s', target: 0 },
  ];
}

export const options = {
  scenarios: {
    autores: { executor: 'ramping-vus', exec: 'autor', stages: etapas(0.6), gracefulRampDown: '10s' },
    revisores: { executor: 'ramping-vus', exec: 'revisor', stages: etapas(0.25), gracefulRampDown: '10s' },
    administradores: { executor: 'ramping-vus', exec: 'administrador', stages: etapas(0.15), gracefulRampDown: '10s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  if (!PW) {
    throw new Error('Falta la variable PW con la clave de los usuarios de prueba');
  }
  const tokens = {};
  for (const usuario of ['admin', ...AUTORES, ...REVISORES]) {
    const r = http.post(
      `${KC}/realms/bancopreguntas/protocol/openid-connect/token`,
      { grant_type: 'password', client_id: 'bancopreguntas-web', username: usuario, password: PW },
      { tags: { name: 'login' } },
    );
    if (r.status !== 200) {
      throw new Error(`No se pudo iniciar sesión como ${usuario}: ${r.status}`);
    }
    tokens[usuario] = r.json('access_token');
  }
  return tokens;
}

const cabeceras = (token) => ({ headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } });

function ok(respuesta, nombre, esperado = 200) {
  const bien = check(respuesta, { [`${nombre} → ${esperado}`]: (r) => r.status === esperado });
  if (!bien && __ENV.DEPURAR && Math.random() < 0.01) {
    console.log(`FALLO : estado=${respuesta.status} error=${respuesta.error} cuerpo=${String(respuesta.body).slice(0, 120)}`);
  }
  return bien;
}

function pausa() {
  sleep(PAUSA + Math.random() * 2 * PAUSA); // tiempo de lectura de una persona
}

function borrador(unico) {
  return JSON.stringify({
    nombre: `Carga ${unico}`,
    contexto: 'Una aplicación debe actualizar varias ventanas cuando cambian los datos de una pregunta.',
    enunciado: '¿Qué patrón notifica a varios objetos cuando cambia el estado de otro?',
    opcionA: 'Factory', opcionB: 'Observer', opcionC: 'Singleton', opcionD: 'Adapter',
    respuestaCorrecta: 'B',
    justificacion: 'Observer define una dependencia uno a muchos para notificar los cambios de estado.',
    bibliografia: 'Gamma, E. et al. (1994). Design Patterns. Addison-Wesley.',
    competencia: 'LECTURA_CRITICA', tema: 'Patrones de diseño', subtema: 'Observer', dificultad: 'INTERMEDIO',
  });
}

export function autor(tokens) {
  const p = cabeceras(tokens[AUTORES[__VU % AUTORES.length]]);
  ok(http.get(`${BASE}/api/questions?size=10`, { ...p, tags: { name: 'mis-preguntas' } }), 'mis preguntas');
  pausa();
  ok(http.get(`${BASE}/api/notifications?size=10`, { ...p, tags: { name: 'notificaciones' } }), 'notificaciones');
  pausa();
  if (__ITER % 40 === 0) { // crear preguntas es poco frecuente: así los autores no acumulan miles de borradores
    ok(http.post(`${BASE}/api/questions`, borrador(`${__VU}-${__ITER}`), { ...p, tags: { name: 'crear-borrador' } }), 'crear borrador', 201);
    pausa();
  }
}

export function revisor(tokens) {
  const p = cabeceras(tokens[REVISORES[__VU % REVISORES.length]]);
  ok(http.get(`${BASE}/api/reviews/assigned`, { ...p, tags: { name: 'bandeja' } }), 'bandeja');
  pausa();
  ok(http.get(`${BASE}/api/reviews/config`, { ...p, tags: { name: 'config-revision' } }), 'configuración');
  ok(http.get(`${BASE}/api/notifications/unread-count`, { ...p, tags: { name: 'no-leidas' } }), 'no leídas');
  pausa();
  ok(http.get(`${BASE}/api/history/me?size=10`, { ...p, tags: { name: 'historial' } }), 'historial');
  pausa();
}

export function administrador(tokens) {
  const p = cabeceras(tokens.admin);
  ok(http.get(`${BASE}/api/reviews/pending-assignment`, { ...p, tags: { name: 'pendientes' } }), 'pendientes');
  pausa();
  ok(http.get(`${BASE}/api/users?rol=REVIEWER&size=20`, { ...p, tags: { name: 'revisores' } }), 'revisores');
  pausa();
  ok(http.get(`${BASE}/api/questions/all?size=10`, { ...p, tags: { name: 'todas-las-preguntas' } }), 'todas las preguntas');
  pausa();
  ok(http.get(`${BASE}/api/audit?size=10`, { ...p, tags: { name: 'auditoria' } }), 'auditoría');
  pausa();
}
