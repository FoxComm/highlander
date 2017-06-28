import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import { expect } from 'chai';
import * as step from '../helpers/steps';

export default ({ objectType, createObject, selectId = obj => obj.id }) => {
	it('[bvt] Can list notes', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newObject = await createObject(api);
		const notes = await step.getNotes(api, objectType, selectId(newObject));
		expect(notes).to.be.a('array');
	});

	it('[bvt] Can create a new note', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newObject = await createObject(api);
		const payload = $.randomCreateNotePayload();
		const newNote = await step.newNote(api, objectType, selectId(newObject), payload);
		expect(newNote.id).to.exist;
		expect(newNote.body).to.equal(payload.body);
		expect(newNote.author.name).to.equal($.adminName);
		expect(newNote.author.email).to.equal($.adminEmail);
		expect(isDate(newNote.createdAt)).to.be.true;
	});

	it('[bvt] Can update note details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newObject = await createObject(api);
		const newNote = await step.newNote(api, objectType, selectId(newObject), $.randomCreateNotePayload());
		const payload = $.randomUpdateNotePayload();
		const updatedNote = await step.updateNote(api, objectType, selectId(newObject), newNote.id, payload);
		expect(updatedNote.id).to.exist;
		expect(updatedNote.body).to.equal(payload.body);
		expect(updatedNote.author.name).to.equal($.adminName);
		expect(updatedNote.author.email).to.equal($.adminEmail);
		expect(isDate(updatedNote.createdAt)).to.be.true;
	});
};
