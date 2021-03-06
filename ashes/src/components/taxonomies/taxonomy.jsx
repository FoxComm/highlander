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

// page layout
import layout from './layout.json';

export type TaxonomyParams = {
  taxonomyId: string,
  context: string,
};

type Props = {
  schema: ?ObjectSchema,
  actions: ObjectActions<Taxonomy>,
  fetchSchema: (namespace: string) => Promise<*>,
  children: Element<*>,
  taxonomy: ?Taxonomy,
  fetchState: AsyncState,
  createState: AsyncState,
  updateState: AsyncState,
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
    const { taxonomy, fetchState, createState, updateState } = nextProps;

    if (!fetchState.inProgress && !createState.inProgress && !updateState.inProgress) {
      this.setState({ taxonomy });
    }
  }

  get navLinks(): NavLinks<TaxonomyParams> {
    return [{
      title: 'Details',
      to: 'taxonomy-details',
      params: this.props.params,
      key: 'taxonomy-details-link',
    }, {
      title: 'Values',
      to: 'values',
      params: this.props.params,
      key: 'values-list-link',
    }];
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

  get fetchState(): AsyncState {
    const { taxonomy, schema, fetchState, schemaFetchState } = this.props;

    const inProgress = fetchState.inProgress || schemaFetchState.inProgress;
    const noError = (!taxonomy && !fetchState.err) || (!schema && !schemaFetchState.err);

    return {
      ...fetchState,
      inProgress: inProgress || noError,
    };
  }

  get saveState(): AsyncState {
    return {
      inProgress: this.props.createState.inProgress || this.props.updateState.inProgress,
      err: this.props.createState.err || this.props.updateState.err,
      finished: this.props.createState.finished || this.props.updateState.finished,
    };
  }

  @autobind
  handleObjectUpdate(obj: Taxonomy) {
    const { taxonomy } = this.state;

    if (taxonomy) {
      const newTaxonomy = assoc(taxonomy, 'attributes', obj.attributes, 'hierarchical', obj.hierarchical);

      this.setState({ taxonomy: newTaxonomy });
    }
  }

  render() {
    const { schema, taxonomy, archiveState, children } = this.props;
    const { taxonomyId, context } = this.props.params;

    return (
      <ObjectPageDeux
        layout={layout}
        schema={schema}
        actions={this.actions}
        context={context}
        identifier={taxonomyId}
        fetchState={this.fetchState}
        saveState={this.saveState}
        archiveState={archiveState}
        navLinks={this.navLinks}
        object={this.state.taxonomy}
        objectType="taxonomy"
        originalObject={taxonomy}
        onUpdateObject={this.handleObjectUpdate}
      >
        {React.cloneElement(children, { taxonomy: this.state.taxonomy })}
      </ObjectPageDeux>
    );
  }
}

const mapState = state => ({
  taxonomy: state.taxonomies.details.taxonomy,
  fetchState: get(state.asyncActions, 'fetchTaxonomy', {}),
  createState: get(state.asyncActions, 'createTaxonomy', {}),
  updateState: get(state.asyncActions, 'updateTaxonomy', {}),
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
