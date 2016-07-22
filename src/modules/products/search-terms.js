/**
 * @flow
 */

const searchTerms = [
  {
    title: 'Product : ID',
    type: 'term',
    term: 'productId',
  },
  {
    title: 'Product : Name',
    type: 'string',
    term: 'title',
  },
  {
    title: 'Product : Active From',
    type: 'date',
    term: 'activeFrom',
  },
  {
    title: 'Product : Active To',
    type: 'date',
    term: 'activeTo',
  },
];

export default searchTerms;
