const searchTerms = [
  {
    title: 'Customer',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string-not-analyzed',
        term: 'name'
      }, {
        title: 'Email',
        type: 'string',
        term: 'email'
      }
    ]
  }
];

export default searchTerms;
