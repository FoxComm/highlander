// @flow

const searchTerms = [
  {
    title: 'Taxonomy : ID',
    type: 'identifier',
    term: 'taxonomyId',
  }, {
    title: 'Taxonomy : Name',
    type: 'string',
    term: 'name',
  }, {
    title: 'Taxonomy : Type',
    type: 'enum',
    term: 'type',
    suggestions: [
      { display: 'Flat', value: 'flat' },
      { display: 'Hierarchical', value: 'hierarchical' },
    ],
  }, {
    title: 'Taxonomy : Active From',
    type: 'date',
    term: 'activeFrom',
  }, {
    title: 'Taxonomy : Active To',
    type: 'date',
    term: 'activeTo',
  }, {
    title: 'Taxonomy : Archived At',
    type: 'date',
    term: 'archivedAt',
  }, {
    title: 'Taxonomy : Is Archived',
    type: 'exists',
    term: 'archivedAt',
    suggestions: [
      { display: 'Yes', operator: 'exists' },
      { display: 'No', operator: 'missing' },
    ],
  },
];

export default searchTerms;
