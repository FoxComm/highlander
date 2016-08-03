/**
 * @flow
 */

const searchTerms = [
  {
    title: 'Product : ID',
    type: 'identifier',
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
  {
    title: 'Product : Archived At',
    type: 'date',
    term: 'archivedAt',
  },
  {
    title: 'Product : Is Archived',
    type: 'exists',
    term: 'archivedAt',
    suggestions: [
      { display: 'Yes', operator: 'exists' },
      { display: 'No', operator: 'missing' },
    ],
  },
];

export default searchTerms;
