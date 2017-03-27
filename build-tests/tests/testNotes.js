import test from '../helpers/test';
import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';

export default ({ objectType, createObject, selectId = obj => obj.id }) => {
  test('Can list notes', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(adminApi);
    const notes = await adminApi.notes.list(objectType, selectId(newObject));
    t.truthy(isArray(notes));
  });

  test('Can create a new note', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const newObject = await createObject(adminApi);
    const payload = $.randomCreateNotePayload();
    const newNote = await adminApi.notes.create(objectType, selectId(newObject), payload);
    t.truthy(newNote.id);
    t.is(newNote.body, payload.body);
    t.is(newNote.author.name, $.adminName);
    t.is(newNote.author.email, $.adminEmail);
    t.truthy(isDate(newNote.createdAt));
  });

  test('Can update note details', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const newObject = await createObject(adminApi);
    const newNote = await adminApi.notes.create(objectType, selectId(newObject), $.randomCreateNotePayload());
    const payload = $.randomUpdateNotePayload();
    const updatedNote = await adminApi.notes.update(objectType, selectId(newObject), newNote.id, payload);
    t.truthy(updatedNote.id);
    t.is(updatedNote.body, payload.body);
    t.is(updatedNote.author.name, $.adminName);
    t.is(updatedNote.author.email, $.adminEmail);
    t.truthy(isDate(updatedNote.createdAt));
  });
};
