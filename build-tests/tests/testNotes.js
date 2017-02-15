import test from '../helpers/test';
import Api from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';

export default ({ objectType, createObject, selectId = obj => obj.id }) => {
  test('Can list notes', async (t) => {
    const api = Api.withCookies();
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(api);
    const notes = await api.notes.list(objectType, selectId(newObject));
    t.truthy(isArray(notes));
  });

  test('Can create a new note', async (t) => {
    const api = Api.withCookies();
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(api);
    const payload = $.randomCreateNotePayload();
    const newNote = await api.notes.create(objectType, selectId(newObject), payload);
    t.truthy(newNote.id);
    t.is(newNote.body, payload.body);
    t.is(newNote.author.name, $.adminName);
    t.is(newNote.author.email, $.adminEmail);
    t.truthy(isDate(newNote.createdAt));
  });

  test('Can update note details', async (t) => {
    const api = Api.withCookies();
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(api);
    const newNote = await api.notes.create(objectType, selectId(newObject), $.randomCreateNotePayload());
    const payload = $.randomUpdateNotePayload();
    const updatedNote = await api.notes.update(objectType, selectId(newObject), newNote.id, payload);
    t.truthy(updatedNote.id);
    t.is(updatedNote.body, payload.body);
    t.is(updatedNote.author.name, $.adminName);
    t.is(updatedNote.author.email, $.adminEmail);
    t.truthy(isDate(updatedNote.createdAt));
  });
};
