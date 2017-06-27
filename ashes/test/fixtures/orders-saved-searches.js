module.exports = [
  {
    name: 'Remorse Hold',
    searches: [
      {
        display: 'Order : State : Remorse Hold',
        term: 'state',
        operator: 'eq',
        value: {
          type: 'enum',
          value: 'remorseHold',
        },
      },
    ],
  },
];
