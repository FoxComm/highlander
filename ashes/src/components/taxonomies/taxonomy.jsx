// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import _ from 'lodash';

// components
import ObjectPageDeux from 'components/object-page/object-page-deux';

// actions
import * as TaxonomyActions from 'modules/taxonomies/details';

// types
import type { ObjectActions } from 'components/object-page/object-page-deux';
import type { Taxonomy } from 'paragons/taxonomy';

type TaxonomyParams = {
  taxonomyId: string,
  context: string,
};

type Props = {
  actions: ObjectActions<Taxonomy>,
  children?: Element|Array<Element>,
  details: {
    taxonomy: ?Taxonomy,
  },
  isFetching: boolean,
  fetchError: ?Object,
  params: TaxonomyParams,
};

const schema = {
  "type": "object",
  "title": "Taxonomy",
  "$schema": "http://json-schema.org/draft-04/schema#",
  "properties": {
    "attributes": {
      "type": "object",
      "required": [
        "name"
      ],
      "properties": {
        "name": {
          "type": "string",
          "minLength": 1
        },
        "activeTo": {
          "type": [
            "string",
            "null"
          ],
          "format": "date-time"
        },
        "activeFrom": {
          "type": [
            "string",
            "null"
          ],
          "format": "date-time"
        },
        "description": {
          "type": "string",
          "widget": "richText"
        },
      },
      "description": "Taxonomy attributes itself"
    }
  }
};

class TaxonomyPage extends Component {
  props: Props;
  state: { taxonomy: ?Taxonomy } = { taxonomy: null };

  componentWillReceiveProps(nextProps: Props) {
    const { taxonomy } = nextProps.details;
    this.setState({ taxonomy });
  }

  get actions(): ObjectActions<Taxonomy> {
    const { reset, fetch, create, update, archive } = this.props.actions;

    return {
      reset,
      fetch,
      create,
      update,
      archive,
      cancel: () => transitionTo('taxonomies'),
      getTitle: (t: Taxonomy) => _.get(t.attributes, 'name.v', ''),
    };
  }

  get isFetching(): boolean {
    const { isFetching, fetchError } = this.props;
    const { taxonomy } = this.props.details;

    return isFetching || (!taxonomy && !fetchError);
  }

  get navLinks(): NavLinks<TaxonomyParams> {
    return [{
      title: 'Details',
      to: 'taxonomy-details',
      params: this.props.params,
      key: 'taxonomy-details-link',
    }, {
      title: 'Values',
      to: 'taxons',
      params: this.props.params,
      key: 'taxons-list-link',
    }];
  }

  @autobind
  handleObjectUpdate(obj: ObjectView) {
    const { taxonomy } = this.state;
    if (taxonomy) {
      const { attributes } = obj;
      const newTaxonomy = {
        ...taxonomy,
        attributes: { ...taxonomy.attributes, ...attributes },
      };
      this.setState({ taxonomy: newTaxonomy });
    }
  }

  render(): Element {
    const { taxonomyId, context } = this.props.params;
    const childProps = {
      schema: schema,
      taxonomy: this.state.taxonomy,
      onUpdateObject: this.handleObjectUpdate,
    };
    const children = React.cloneElement(this.props.children, childProps);

    return (
      <ObjectPageDeux
        actions={this.actions}
        context={context}
        identifier={taxonomyId}
        isFetching={this.isFetching}
        fetchError={this.props.fetchError}
        navLinks={this.navLinks}
        object={this.state.taxonomy}
        objectType="taxonomy"
        originalObject={this.props.details.taxonomy}
      >
        {children}
      </ObjectPageDeux>
    );
  }
}

const mapStateToProps = state => {
  return {
    details: state.taxonomies.details,
    isFetching: _.get(state.asyncActions, 'fetchTaxonomy.inProgress', true),
    fetchError: _.get(state.asyncActions, 'fetchTaxonomy.err', null),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(TaxonomyActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(TaxonomyPage);
