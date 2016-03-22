import ConditionAnd from './condition-and';
import ConditionOr from './condition-or';
import Field from './field';


export default class Query {

  constructor(criterions) {
    this._criterions = criterions;
    this._main = null;
  }

  toRequest() {
    return {
      filter: {
        bool: this.main.toRequest()
      },
    };
  }

  and() {
    return new ConditionAnd(this._criterions);
  }

  or() {
    return new ConditionOr(this._criterions);
  }

  field(name) {
    return new Field(this._criterions, name);
  }

  get main() {
    return this._main;
  }

  set main(value) {
    this._main = value;
  }

}
