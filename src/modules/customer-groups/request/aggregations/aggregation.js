/* @flow */

import Element from '../element';


export default class Aggregation extends Element {

  name: string;

  toRequest: () => Object;

  constructor(name: string) {
    super();
    this.name = name;
  }

}

/*
 var request = new Request(criterions);
 request.aggregations
 .add(
 new Aggregations.Histogram({interval: 5000})
 .add( //adds nested aggregation, defined only for bucket aggregations
 new Aggregations.Average({field: 'test'})
 )
 );

 */
