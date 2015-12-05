import _ from 'lodash';
import nock from 'nock';

const SearchTerm = importSource('paragons/search-term.js');

describe('paragons.searchTerm', function() {
  const jsonTerms = [
    {
      term: 'String Term',
      type: 'string'
    }, {
      term: 'Number Term',
      type: 'number'
    }, {
      term: 'Date Term',
      type: 'date'
    }, {
      term: 'Nested Term',
      type: 'object',
      options: [{
        term: 'Child Term',
        type: 'string'
      }]
    }, {
      term: 'Enum Term',
      type: 'enum',
      suggestions: ['One', 'Two', 'Three']
    }
  ];

  let terms = null;
  beforeEach(function() {
    terms = jsonTerms.map(t => new SearchTerm(t));
  });

  it('should create all the terms correctly', function() {
    expect(terms).to.have.length(jsonTerms.length);
  });

  describe('.displayTerm', function() {
    it('should use the term as the display term', function() {
      _.forEach(terms, (term, idx) => {
        expect(term.displayTerm).to.be.equal(jsonTerms[idx].term);
      });
    });
  });

  describe('.displayAction', function() {
    it('should display the actions as search', function() {
      _.forEach(terms, term => expect(term.displayAction).to.be.equal('Search'));
    });

    it('should be empty with a value term', function() {
      const valueTerm = new SearchTerm({ term: 'Value Term', type: 'value' });
      expect(valueTerm.displayAction).to.be.equal('');
    });
  });

  describe('.selectionValue', function() {
    it('should append a colon for non-value terms', function() {
      _.forEach(terms, term => {
        expect(term.selectionValue).to.be.equal(`${term.displayTerm} : `);
      });
    });

    it('should display the exact displayValue for value terms', function() {
      const valueTerm = new SearchTerm({ term: 'Value Term', type: 'value' });
      expect(valueTerm.selectionValue).to.be.equal(valueTerm.displayTerm);
    });
  });

  describe('.applicableTerms', function() {
    const nestedTerm = new SearchTerm({
      term: 'Parent',
      type: 'object',
      options: [
        {
          term: 'Child String',
          type: 'string'
        }, {
          term: 'Child Number',
          type: 'number'
        }
      ]
    });

    const enumTerm = new SearchTerm({
      term: 'An enumeration',
      type: 'enum',
      suggestions: ['One', 'Two', 'Three']
    });

    it('should return all top level term with an empty search', function() {
      const visible = nestedTerm.applicableTerms('');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal(nestedTerm.displayTerm);
    });

    it('should return the top level term with a partial search', function() {
      const visible = nestedTerm.applicableTerms('Par');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal(nestedTerm.displayTerm);
    });

    it('should return nothing with no match', function() {
      const visible = nestedTerm.applicableTerms('asrtoasnrt');
      expect(visible).to.be.empty;
    });

    it('should return term with exact displayTerm match', function() {
      const visible = nestedTerm.applicableTerms(nestedTerm.displayTerm);
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal(nestedTerm.displayTerm);
    });

    it('should return term if there is not space padding after displayTerm', function() {
      const visible = nestedTerm.applicableTerms(nestedTerm.displayTerm.trim());
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal(nestedTerm.displayTerm);
    });

    it('should be match when there is extra whitespace at end of search', function() {
      const visible = nestedTerm.applicableTerms(`${nestedTerm.displayTerm}   `);
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal(nestedTerm.displayTerm);
    });

    it('should return child terms with selectionValue match', function() {
      const visible = nestedTerm.applicableTerms(nestedTerm.selectionValue);
      expect(visible).to.have.length(2);
      expect(visible[0].displayTerm).to.be.equal('Parent : Child String');
      expect(visible[1].displayTerm).to.be.equal('Parent : Child Number');
    });

    it('should return correct child term on partial match', function() {
      const visible = nestedTerm.applicableTerms('Parent : Child Str');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal('Parent : Child String');
    });

    it('should return correct child on full match', function() {
      const visible = nestedTerm.applicableTerms('Parent : Child Number');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal('Parent : Child Number');
    });

    it('should match correct child with missing whitespace around separator', function() {
      const visible = nestedTerm.applicableTerms('Parent:Child Number');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal('Parent : Child Number');
    });

    it('should match correct child with extra whitespace around separator', function() {
      const visible = nestedTerm.applicableTerms('Parent  :  Child Number');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal('Parent : Child Number');
    });

    it('should match correct child with value added to search team', function() {
      const visible = nestedTerm.applicableTerms('Parent : Child Number : 7');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal('Parent : Child Number');
    });

    it('should not show children on partial match of enumeration', function() {
      const visible = enumTerm.applicableTerms('An enum');
      expect(visible).to.have.length(1);
      expect(visible[0].displayTerm).to.be.equal('An enumeration');
    });

    it('should show enumeration values on a full match', function() {
      const visible = enumTerm.applicableTerms(enumTerm.selectionValue);
      expect(visible).to.have.length(3);
      expect(visible[0].displayTerm).to.be.equal('An enumeration : One');
      expect(visible[1].displayTerm).to.be.equal('An enumeration : Two');
      expect(visible[2].displayTerm).to.be.equal('An enumeration : Three');
    });

    it('should not match an enumeration with an invalid value', function() {
      const visible = enumTerm.applicableTerms('An enumeration : Four');
      expect(visible).to.be.empty;
    });
  });

  describe('.selectTerm', function() {
    const nestedTerm = new SearchTerm({
      term: 'Parent',
      type: 'object',
      options: [
        {
          term: 'Child String',
          type: 'string'
        }, {
          term: 'Child Number',
          type: 'number'
        }
      ]
    });

    const enumTerm = new SearchTerm({
      term: 'An enumeration',
      type: 'enum',
      suggestions: ['One', 'Two', 'Three']
    });

    it('should not select a parent', function() {
      expect(nestedTerm.selectTerm('Parent')).to.be.false;
    });

    it('should not select a child without a value', function() {
      const search = 'Parent : Child String';
      const toSelect = nestedTerm.applicableTerms(search);
      expect(toSelect[0].selectTerm(search)).to.be.false;
    });

    it('should select a child with a value', function() {
      const search = 'Parent : Child String : some string';
      const toSelect = nestedTerm.applicableTerms(search);
      expect(toSelect[0].selectTerm(search)).to.be.true;
    });

    it('should not select an enum', function() {
      expect(enumTerm.selectTerm('An enumeration')).to.be.false;
    });

    it('should select an enum with a pre-defined value', function() {
      const search = 'An enumeration : One';
      const toSelect = enumTerm.applicableTerms(search);
      expect(toSelect[0].selectTerm(search)).to.be.true;
    });
  });
});
