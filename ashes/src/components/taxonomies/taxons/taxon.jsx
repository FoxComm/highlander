// @flow

// lib
import React, { Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { get } from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import ObjectPageDeux from 'components/object-page/object-page-deux';
import { AddButton } from 'components/core/button';

// actions
import { fetch as fetchTaxonomy } from 'modules/taxonomies/details';
import * as taxonsActions from 'modules/taxons/details/taxon';

// helpers
import { transitionTo, transitionToLazy } from 'browserHistory';

import s from './taxons.css';


// page layout
import layout from './layout';

export type TaxonParams = {
  taxonomyId: string,
  context: string,
  taxonId: string
};

type Props = {
  actions: ObjectActions<Taxon>,
  children: Element<*>,
  taxon: Taxon,
  taxonomy: Taxonomy,
  fetchState: AsyncState,
  createState: AsyncState,
  updateState: AsyncState,
  archiveState: AsyncState,
  archiveState: AsyncState,
  fetchStateTaxonomy: AsyncState,
  fetchTaxonomy: (id: number) => Promise<*>,
  addSubvalue: (parentId: number) => void,
  params: TaxonParams,
};

type State = {
  taxon: ?Taxon
};

const schema: ObjectSchema = {
  type: 'object',
  title: 'Taxon',
  $schema: 'http://json-schema.org/draft-04/schema#',
  properties: {
    attributes: {
      type: 'object',
      required: ['name'],
      properties: {
        name: {
          type: 'string',
          minLength: 1
        },
        description: {
          type: 'string',
          widget: 'richText'
        },
      },
    },
    location: {},
    description: 'Taxon attributes'
  }
};

class TaxonPage extends React.Component {
  props: Props;
  state: State = {
    taxon: this.props.taxon,
  };

  componentDidMount() {
    const taxonomyParam = parseInt(this.props.params.taxonomyId, 10);

    if (this.props.taxonomy.id !== taxonomyParam) {
      this.props.fetchTaxonomy(taxonomyParam);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    const { taxon, fetchState, createState, updateState } = nextProps;

    if (!fetchState.inProgress && !createState.inProgress && !updateState.inProgress) {
      this.setState({ taxon });
    }
  }

  get isNew() {
    return this.props.params.taxonId === 'new';
  }

  get actions(): ObjectActions<Taxon> {
    return {
      ...this.props.actions,
      close: transitionToLazy('values', {
        taxonomyId: this.props.params.taxonomyId,
        context: this.props.params.context
      }),
      getTitle: (t: Taxon) => get(t.attributes, 'name.v', ''),
      transition: (id: number|string) => transitionTo('taxon-details', {
        taxonomyId: this.props.params.taxonomyId,
        context: this.props.params.context,
        taxonId: id
      })
    };
  }

  get navLinks(): NavLinks<TaxonParams> {
    return [
      { title: 'Details', to: 'taxon-details', params: this.props.params },
      { title: 'Products', to: 'taxon-products', params: this.props.params },
    ];
  }

  get fetchState(): AsyncState {
    const { taxonomy, taxon, fetchState, fetchStateTaxonomy } = this.props;

    const inProgress = fetchState.inProgress || fetchStateTaxonomy.inProgress;
    const notStarted = !inProgress && (!taxonomy.id || (!this.isNew && !taxon.id) || !schema);
    const err = fetchState.err || fetchStateTaxonomy.err;
    const finished = (!this.isNew && fetchState.finished) && fetchStateTaxonomy.finished;

    return {
      inProgress: inProgress || notStarted,
      finished,
      err,
    };
  }

  get saveState(): AsyncState {
    return {
      inProgress: this.props.createState.inProgress || this.props.updateState.inProgress,
      finished: this.props.createState.finished || this.props.updateState.finished,
      err: this.props.createState.err || this.props.updateState.err,
    };
  }

  @autobind
  handleObjectUpdate(obj) {
    const { taxon } = this.state;

    const newTaxon = assoc(taxon,
      'attributes', obj.attributes,
      'location', obj.location,
    );

    this.setState({ taxon: newTaxon });
  }

  @autobind
  handleAddSubvalue() {
    this.props.addSubvalue(this.props.taxon.id);

    transitionTo('taxon-details', { ...this.props.params, taxonId: 'new' });
  }

  get headerControls() {
    const hierarchical = get(this.props.taxonomy, 'hierarchical');
    if (this.isNew || !hierarchical) {
      return;
    }

    return [
      <AddButton
        className={s.subValue}
        onClick={this.handleAddSubvalue}
        children={'Subvalue'}
        key="subvalue-btn"
      />,
    ];
  }

  render() {
    const { taxonomy, taxon, archiveState, params: { taxonId, context }, children }  = this.props;

    return (
      <ObjectPageDeux
        context={context}
        layout={layout(taxonomy)}
        schema={schema}
        identifier={taxonId}
        object={this.state.taxon}
        objectType="value"
        internalObjectType="taxon"
        originalObject={taxon}
        actions={this.actions}
        onUpdateObject={this.handleObjectUpdate}
        fetchState={this.fetchState}
        saveState={this.saveState}
        archiveState={archiveState}
        navLinks={this.navLinks}
        headerControls={this.headerControls}
      >
        {React.cloneElement(children, { taxonomy })}
      </ObjectPageDeux>
    );
  }
}

const mapState = state => ({
  taxon: state.taxons.details.taxon,
  taxonomy: state.taxonomies.details.taxonomy,
  fetchState: get(state.asyncActions, 'fetchTaxon', {}),
  createState: get(state.asyncActions, 'createTaxon', {}),
  updateState: get(state.asyncActions, 'updateTaxon', {}),
  archiveState: get(state.asyncActions, 'archiveTaxon', {}),
  fetchStateTaxonomy: get(state.asyncActions, 'fetchTaxonomy', {}),
});

const mapActions = (dispatch, props) => ({
  actions: {
    ...bindActionCreators(taxonsActions, dispatch),
    create: bindActionCreators(taxonsActions.create(props.params.taxonomyId), dispatch),
  },
  fetchTaxonomy: bindActionCreators(fetchTaxonomy, dispatch),
  addSubvalue: bindActionCreators(taxonsActions.addSubvalue, dispatch),
});

export default connect(mapState, mapActions)(TaxonPage);

