// @flow

// libs
import { get } from 'lodash';
import { assoc } from 'sprout-data';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { transitionTo, transitionToLazy } from 'browserHistory';

// components
import ObjectPageDeux from 'components/object-page/object-page-deux';

// actions
import { fetchSchema } from 'modules/object-schema';
import * as taxonomiesActions from 'modules/taxonomies/details';

type TaxonomyParams = {
  taxonomyId: string,
  context: string,
};

type Props = {
  schema: ?Object,
  actions: ObjectActions<Taxonomy>,
  fetchSchema: (namespace: string) => Promise<*>,
  children: Element<*>,
  details: {
    taxonomy: ?Taxonomy,
  },
  fetchState: AsyncState,
  schemaFetchState: AsyncState,
  archiveState: AsyncState,
  params: TaxonomyParams,
};

type State = {
  taxonomy: ?Taxonomy
};

class TaxonomyPage extends Component {
  props: Props;
  state: State = { taxonomy: null };

  componentDidMount() {
    if (!this.props.schema) {
      this.props.fetchSchema('taxonomy');
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    const { taxonomy } = nextProps.details;

    this.setState({ taxonomy });
  }

  get actions(): ObjectActions<Taxonomy> {
    return {
      ...this.props.actions,
      close: transitionToLazy('taxonomies'),
      getTitle: (t: Taxonomy) => get(t.attributes, 'name.v', ''),
      transition: (id: number|string) => transitionTo('taxonomy-details', {
        taxonomyId: id,
        context: this.props.params.context
      })
    };
  }

  get isFetching(): boolean {
    const { details, schema, fetchState, schemaFetchState } = this.props;

    const inProgress = fetchState.inProgress || schemaFetchState.inProgress;
    const noError = (!details.taxonomy && !fetchState.err) || (!schema && !schemaFetchState.err);

    return inProgress || noError;
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
      const newTaxonomy = assoc(taxonomy, 'attributes', obj.attributes);

      this.setState({ taxonomy: newTaxonomy });
    }
  }

  render() {
    const { details, fetchState, archiveState, schema } = this.props;
    const { taxonomyId, context } = this.props.params;


    const childProps = {
      schema,
      taxonomy: this.state.taxonomy,
      onUpdateObject: this.handleObjectUpdate,
    };

    const children = React.cloneElement(this.props.children, childProps);

    return (
      <ObjectPageDeux
        actions={this.actions}
        context={context}
        identifier={get(this.props.details.taxonomy, 'id', taxonomyId)}
        isFetching={this.isFetching}
        fetchError={fetchState.err}
        navLinks={this.navLinks}
        object={this.state.taxonomy}
        objectType="taxonomy"
        originalObject={details.taxonomy}
        archiveState={archiveState}
      >
        {children}
      </ObjectPageDeux>
    );
  }
}

const mapState = state => ({
  details: state.taxonomies.details,
  fetchState: get(state.asyncActions, 'fetchTaxonomy', {}),
  archiveState: get(state.asyncActions, 'archiveTaxonomy', {}),
  schemaFetchState: get(state.asyncActions, 'fetchSchema', {}),
  schema: get(state.objectSchemas, 'taxonomy'),
});

const mapActions = dispatch => ({
  actions: {
    ...bindActionCreators(taxonomiesActions, dispatch),
  },
  fetchSchema: bindActionCreators(fetchSchema, dispatch),
});

export default connect(mapState, mapActions)(TaxonomyPage);
