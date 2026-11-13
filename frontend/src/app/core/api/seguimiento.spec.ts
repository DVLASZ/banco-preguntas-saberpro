import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '../config';
import { ReportsService } from './reports.service';
import { SimulationsService } from './simulations.service';
import { StatisticsService } from './statistics.service';

const API = 'http://api.test';

describe('servicios de simulacros y seguimiento', () => {
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

  describe('SimulationsService', () => {
    it('el resumen del catálogo viene del servicio de catálogo, no del de simulacros', () => {
      TestBed.inject(SimulationsService).catalogo().subscribe();

      controlador.expectOne(`${API}/api/simulations/catalog/summary`).flush({ total: 0, porCompetencia: {}, porDificultad: {} });
    });

    it('crea el simulacro con los criterios, lista los míos y consulta uno', () => {
      const servicio = TestBed.inject(SimulationsService);

      servicio.crear({ competencias: ['INGLES'], dificultades: [], cantidad: 5, duracionMinutos: 20 }).subscribe();
      const crear = controlador.expectOne(`${API}/api/simulations`);
      expect(crear.request.method).toBe('POST');
      expect(crear.request.body).toEqual({ competencias: ['INGLES'], dificultades: [], cantidad: 5, duracionMinutos: 20 });
      crear.flush({});

      servicio.mios(1, 8).subscribe();
      const lista = controlador.expectOne((req) => req.url === `${API}/api/simulations`);
      expect(lista.request.params.get('page')).toBe('1');
      expect(lista.request.params.get('size')).toBe('8');
      lista.flush({});

      servicio.obtener('s-1').subscribe();
      controlador.expectOne(`${API}/api/simulations/s-1`).flush({});
    });

    it('responde una pregunta y finaliza el simulacro', () => {
      const servicio = TestBed.inject(SimulationsService);

      servicio.responder('s-1', 'q-1', 'B').subscribe();
      const respuesta = controlador.expectOne(`${API}/api/simulations/s-1/answers`);
      expect(respuesta.request.body).toEqual({ preguntaId: 'q-1', opcion: 'B' });
      respuesta.flush({});

      servicio.finalizar('s-1').subscribe();
      const fin = controlador.expectOne(`${API}/api/simulations/s-1/finish`);
      expect(fin.request.method).toBe('POST');
      fin.flush({});
    });
  });

  describe('StatisticsService', () => {
    it('consulta lo propio por /me y lo de un estudiante por /students/{id}', () => {
      const servicio = TestBed.inject(StatisticsService);

      servicio.misEstadisticas().subscribe();
      controlador.expectOne(`${API}/api/tracking/me/stats`).flush({});
      servicio.misCompetencias().subscribe();
      controlador.expectOne(`${API}/api/tracking/me/competencies`).flush({});
      servicio.miHistorial(2, 5).subscribe();
      const historial = controlador.expectOne((req) => req.url === `${API}/api/tracking/me/history`);
      expect(historial.request.params.get('page')).toBe('2');
      historial.flush({});

      servicio.estadisticasDe('est 1').subscribe();
      controlador.expectOne(`${API}/api/tracking/students/est%201/stats`).flush({});
      servicio.competenciasDe('est1').subscribe();
      controlador.expectOne(`${API}/api/tracking/students/est1/competencies`).flush({});
      servicio.historialDe('est1').subscribe();
      controlador.expectOne((req) => req.url === `${API}/api/tracking/students/est1/history`).flush({});
    });
  });

  describe('ReportsService', () => {
    it('pide el reporte del grupo y su versión CSV como archivo', () => {
      const servicio = TestBed.inject(ReportsService);

      servicio.grupo().subscribe();
      controlador.expectOne(`${API}/api/tracking/reports/group`).flush({});

      servicio.grupoCsv().subscribe();
      const csv = controlador.expectOne(`${API}/api/tracking/reports/group.csv`);
      expect(csv.request.responseType).toBe('blob');
      csv.flush(new Blob(['a,b']));
    });
  });
});
