export default class Condition {

  constructor(criterions) {
    this._criterions = criterions;
    this._conditions = [];
  }

  set(conditions) {
    this._conditions = conditions;

    return this;
  }

  add(condition) {
    this._conditions.push(condition);

    return this;
  }

}
