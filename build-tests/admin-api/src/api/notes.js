
// @class Notes
// Accessible via [notes](#foxapi-notes) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Notes {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(objectType: String, objectIdOrCode: String|Number): Promise<AdminNote[]>
   * List notes.
   */
  list(objectType, objectIdOrCode) {
    return this.api.get(endpoints.notes(objectType, objectIdOrCode));
  }

  /**
   * @method create(objectType: String, objectIdOrCode: String|Number, note: NotePayload): Promise<AdminNote>
   * Create a note.
   */
  create(objectType, objectIdOrCode, note) {
    return this.api.post(endpoints.notes(objectType, objectIdOrCode), note);
  }

  /**
   * @method update(objectType: String, objectIdOrCode: String|Number, noteId: Number, note: NotePayload): Promise<AdminNote>
   * Update note details.
   */
  update(objectType, objectIdOrCode, noteId, note) {
    return this.api.patch(endpoints.note(objectType, objectIdOrCode, noteId), note);
  }
}
