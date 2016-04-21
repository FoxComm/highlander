export default class Sorter {

  constructor(criterions) {
    super(criterions);
  }

  toRequest() {
    if (this._conditions.length === 1) {
      return this._conditions[0].toRequest();
    }

    const result = [];
    for (const condition of this._conditions) {
      result.push({bool: condition.toRequest()});
    }

    return {
      [booleanOperators.and]: result,
    };
  }

}
