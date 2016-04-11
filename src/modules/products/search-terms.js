const searchTerms = [
  {
    title: 'Product : ID',
    type: 'term',
    term: 'id'
  },
  {
    title: 'Product : Name',
    type: 'string',
    term: 'name'
  },
  {
    title: 'Product : Active From',
    type: 'date',
    term: 'activefrom',
  },
  {
    title: 'Product : Active To',
    type: 'date',
    term: 'activeto',
  },
];

export default searchTerms;
