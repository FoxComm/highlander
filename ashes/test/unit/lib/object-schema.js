const { expandRefs } = requireSource('lib/object-schema');

describe('object-schema', function() {
  context('#expandRefs', function() {
    it('expand nested refs inside objects', function() {
      const source = {
        type: 'object',
        definitions: {
          foo: {
            type: 'number',
            minimum: 2,
          },
        },
        properties: {
          foo: {
            $ref: '#/definitions/foo',
          },
        },
      };
      const expected = {
        type: 'object',
        definitions: {
          foo: {
            type: 'number',
            minimum: 2,
          },
        },
        properties: {
          foo: {
            type: 'number',
            minimum: 2,
          },
        },
      };
      expect(expandRefs(source)).to.be.deep.equal(expected);
    });

    it('expand nested refs inside array', function() {
      const source = {
        definitions: {
          bars: {
            type: 'string',
            title: 'foo',
            f: 2,
          },
        },
        type: 'object',
        properties: {
          foo: {
            type: 'array',
            items: {
              $ref: '#/definitions/bars',
            },
          },
        },
      };
      const expected = {
        definitions: {
          bars: {
            type: 'string',
            title: 'foo',
            f: 2,
          },
        },
        type: 'object',
        properties: {
          foo: {
            type: 'array',
            items: {
              type: 'string',
              title: 'foo',
              f: 2,
            },
          },
        },
      };
      expect(expandRefs(source)).to.be.deep.equal(expected);
    });
  });
});
