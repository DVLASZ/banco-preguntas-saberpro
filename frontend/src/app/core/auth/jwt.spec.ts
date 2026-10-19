import { tokenDePrueba } from '../../testing/tokens';
import { iniciales, leerReclamaciones, usuarioDeToken } from './jwt';

describe('jwt', () => {
  it('lee el usuario, el nombre, el correo y los roles de Keycloak', () => {
    const token = tokenDePrueba({
      preferred_username: 'autor1',
      name: 'Andrés Autor Uno',
      email: 'autor1@bancopreguntas.local',
      realm_access: { roles: ['author', 'offline_access'] },
    });

    expect(usuarioDeToken(token)).toEqual({
      id: 'autor1',
      nombre: 'Andrés Autor Uno',
      correo: 'autor1@bancopreguntas.local',
      roles: ['AUTHOR', 'OFFLINE_ACCESS'],
    });
  });

  it('decodifica correctamente caracteres con tildes', () => {
    expect(leerReclamaciones(tokenDePrueba({ name: 'Rocío Peña' }))['name']).toBe('Rocío Peña');
  });

  it('sin preferred_username usa el sujeto, y sin nombre arma uno con el nombre y apellido', () => {
    const token = tokenDePrueba({ sub: 'kc-123', given_name: 'Ana', family_name: 'Pérez' });

    const usuario = usuarioDeToken(token);

    expect(usuario.id).toBe('kc-123');
    expect(usuario.nombre).toBe('Ana Pérez');
    expect(usuario.correo).toBeNull();
    expect(usuario.roles).toEqual([]);
  });

  it('sin ningún nombre usa el identificador', () => {
    expect(usuarioDeToken(tokenDePrueba({ preferred_username: 'u-1' })).nombre).toBe('u-1');
  });

  it('ignora roles mal formados', () => {
    expect(usuarioDeToken(tokenDePrueba({ preferred_username: 'u', realm_access: { roles: 'ADMIN' } })).roles).toEqual([]);
    expect(usuarioDeToken(tokenDePrueba({ preferred_username: 'u', realm_access: { roles: [1, 'REVIEWER'] } })).roles).toEqual(['REVIEWER']);
  });

  it('rechaza lo que no tiene forma de JWT', () => {
    expect(() => leerReclamaciones('no-es-un-token')).toThrow();
  });

  it('calcula las iniciales para el avatar', () => {
    expect(iniciales('Ana Rodríguez')).toBe('AR');
    expect(iniciales('Rafael Revisor Uno')).toBe('RU');
    expect(iniciales('admin')).toBe('AD');
    expect(iniciales('  ')).toBe('?');
  });
});
