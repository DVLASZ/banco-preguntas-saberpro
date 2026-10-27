import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '../config';
import { ContenidoPregunta } from '../models';
import { HistoryService } from './history.service';
import { NotificationsService } from './notifications.service';
import { QuestionsService } from './questions.service';
import { ReviewsService } from './reviews.service';
import { UsersService } from './users.service';

const API = 'http://api.test';

describe('servicios del API', () => {
  let controlador: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: { apiUrl: API, keycloakUrl: 'k', realm: 'r', clientId: 'c' } },
      ],
    });
    controlador = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controlador.verify());

  describe('QuestionsService', () => {
    it('lista mis preguntas con los filtros no vacíos', () => {
      TestBed.inject(QuestionsService).listarMias({ estado: 'RECHAZADA', competencia: '', texto: 'obs', page: 2, size: 10 }).subscribe();

      const r = controlador.expectOne((req) => req.url === `${API}/api/questions`);
      expect(r.request.params.get('estado')).toBe('RECHAZADA');
      expect(r.request.params.get('texto')).toBe('obs');
      expect(r.request.params.get('page')).toBe('2');
      expect(r.request.params.has('competencia')).toBe(false);
      r.flush({ contenido: [], pagina: 2, tamano: 10, totalElementos: 0, totalPaginas: 1 });
    });

    it('lista todas las preguntas en la ruta del administrador', () => {
      TestBed.inject(QuestionsService).listarTodas({ estado: 'APROBADA', autor: 'autor1' }).subscribe();

      const r = controlador.expectOne((req) => req.url === `${API}/api/questions/all`);
      expect(r.request.params.get('autor')).toBe('autor1');
      r.flush({});
    });

    it('crea, actualiza, envía, reabre y publica con el verbo y la ruta de cada operación', () => {
      const servicio = TestBed.inject(QuestionsService);
      const contenido = { nombre: 'N' } as ContenidoPregunta;

      servicio.crear(contenido).subscribe();
      const crear = controlador.expectOne(`${API}/api/questions`);
      expect(crear.request.method).toBe('POST');
      expect(crear.request.body).toEqual(contenido);
      crear.flush({});

      servicio.actualizar('q-1', contenido).subscribe();
      expect(controlador.expectOne(`${API}/api/questions/q-1`).request.method).toBe('PUT');

      for (const [llamar, ruta] of [
        [() => servicio.enviarARevision('q-1'), 'submit'],
        [() => servicio.reabrir('q-1'), 'reopen'],
        [() => servicio.publicar('q-1'), 'publish'],
      ] as const) {
        llamar().subscribe();
        const r = controlador.expectOne(`${API}/api/questions/q-1/${ruta}`);
        expect(r.request.method).toBe('POST');
        r.flush({});
      }
    });
  });

  describe('ReviewsService', () => {
    it('asigna revisores con la lista de identificadores', () => {
      TestBed.inject(ReviewsService).asignar('q-1', ['revisor1', 'revisor2']).subscribe();

      const r = controlador.expectOne(`${API}/api/reviews/q-1/assignments`);
      expect(r.request.body).toEqual({ reviewerIds: ['revisor1', 'revisor2'] });
      r.flush({});
    });

    it('envía la evaluación; una observación en blanco va como nula', () => {
      const servicio = TestBed.inject(ReviewsService);

      servicio.evaluar('q-1', 'APROBAR', '   ').subscribe();
      const aprobar = controlador.expectOne(`${API}/api/reviews/q-1/evaluations`);
      expect(aprobar.request.body).toEqual({ decision: 'APROBAR', observacion: null });
      aprobar.flush({});

      servicio.evaluar('q-1', 'RECHAZAR', '  Falta contexto ').subscribe();
      const rechazar = controlador.expectOne(`${API}/api/reviews/q-1/evaluations`);
      expect(rechazar.request.body).toEqual({ decision: 'RECHAZAR', observacion: 'Falta contexto' });
      rechazar.flush({});
    });

    it('consulta la bandeja y las pendientes con paginación', () => {
      const servicio = TestBed.inject(ReviewsService);

      servicio.bandeja('EVALUADAS', 1, 5).subscribe();
      const bandeja = controlador.expectOne((req) => req.url === `${API}/api/reviews/assigned`);
      expect(bandeja.request.params.get('estado')).toBe('EVALUADAS');
      expect(bandeja.request.params.get('size')).toBe('5');
      bandeja.flush({});

      servicio.pendientesDeAsignacion().subscribe();
      expect(controlador.expectOne((req) => req.url === `${API}/api/reviews/pending-assignment`).request.params.get('page')).toBe('0');
    });
  });

  describe('UsersService', () => {
    it('lista los candidatos a revisor', () => {
      TestBed.inject(UsersService).listar({ rol: 'REVIEWER', soloActivos: true, size: 100 }).subscribe();

      const r = controlador.expectOne((req) => req.url === `${API}/api/users`);
      expect(r.request.params.get('rol')).toBe('REVIEWER');
      expect(r.request.params.get('soloActivos')).toBe('true');
      r.flush({});
    });
  });

  describe('NotificationsService', () => {
    it('mantiene el contador de no leídas al consultar, leer y marcar todas', () => {
      const servicio = TestBed.inject(NotificationsService);

      servicio.refrescarNoLeidas().subscribe();
      controlador.expectOne(`${API}/api/notifications/unread-count`).flush({ noLeidas: 3 });
      expect(servicio.noLeidas()).toBe(3);

      servicio.marcarLeida('n-1').subscribe();
      controlador.expectOne(`${API}/api/notifications/n-1/read`).flush({});
      expect(servicio.noLeidas()).toBe(2);

      servicio.marcarTodasLeidas().subscribe();
      controlador.expectOne(`${API}/api/notifications/read-all`).flush({ marcadas: 2 });
      expect(servicio.noLeidas()).toBe(0);
    });

    it('el contador nunca baja de cero', () => {
      const servicio = TestBed.inject(NotificationsService);

      servicio.marcarLeida('n-1').subscribe();
      controlador.expectOne(`${API}/api/notifications/n-1/read`).flush({});

      expect(servicio.noLeidas()).toBe(0);
    });

    it('consulta los correos simulados con paginación', () => {
      TestBed.inject(NotificationsService).correos(1, 5).subscribe();

      const r = controlador.expectOne((req) => req.url === `${API}/api/emails`);
      expect(r.request.params.get('page')).toBe('1');
      r.flush({});
    });
  });

  describe('HistoryService', () => {
    it('consulta el recorrido de una pregunta, lo mío y la auditoría filtrada', () => {
      const servicio = TestBed.inject(HistoryService);

      servicio.deUnaPregunta('q-1').subscribe();
      controlador.expectOne(`${API}/api/history/questions/q-1`).flush([]);

      servicio.mio(0, 20).subscribe();
      controlador.expectOne((req) => req.url === `${API}/api/history/me`).flush({});

      servicio.auditoria({ tipo: 'INICIO_DE_SESION_FALLIDO', actorId: 'intruso', preguntaId: '' }).subscribe();
      const auditoria = controlador.expectOne((req) => req.url === `${API}/api/audit`);
      expect(auditoria.request.params.get('tipo')).toBe('INICIO_DE_SESION_FALLIDO');
      expect(auditoria.request.params.get('actorId')).toBe('intruso');
      expect(auditoria.request.params.has('preguntaId')).toBe(false);
      auditoria.flush({});
    });
  });
});
