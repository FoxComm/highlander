// @flow

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
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
  details: {
    taxonomy: ?Taxonomy,
  },
  isFetching: boolean,
  fetchError: ?Object,
  params: TaxonomyParams,
};

class TaxonomyDetailsPage extends Component {
  props: Props;

  get actions(): ObjectActions<Taxonomy> {
    const { reset, fetch, create, update, archive } = this.props.actions;
    return {
      reset,
      fetch,
      create,
      update,
      archive,
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
    }];
  }

  render(): Element {
    const { taxonomyId, context } = this.props.params;

    return (
      <ObjectPageDeux
        actions={this.actions}
        context={context}
        identifier={taxonomyId}
        isFetching={this.isFetching}
        fetchError={this.props.fetchError}
        navLinks={this.navLinks}
        object={this.props.details.taxonomy}
        objectType="taxonomy"
      />
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

export default connect(mapStateToProps, mapDispatchToProps)(TaxonomyDetailsPage);
