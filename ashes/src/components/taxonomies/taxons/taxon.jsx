// @flow

// lib
import React, { Element } from 'react';
import { transitionTo, transitionToLazy } from 'browserHistory';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { get } from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import ObjectPageDeux from 'components/object-page/object-page-deux';

// actions
import * as taxonsActions from 'modules/taxons/details/taxon';

// page layout
import layout from './layout.json';

export type TaxonParams = {
  taxonomyId: string,
  context: string,
  taxonId: string
};

type Props = {
  actions: ObjectActions<Taxon>,
  children: Element<*>,
  details: {
    taxon: ?Taxon,
  },
  fetchState: AsyncState,
  createState: AsyncState,
  updateState: AsyncState,
  archiveState: AsyncState,
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
        colorSwatch: {
          type: 'color',
        }
      },
    },
    description: 'Taxon attributes'
  }
};

class TaxonPage extends React.Component {
  props: Props;
  state: State = { taxon: null };

  componentWillReceiveProps(nextProps: Props) {
    const { fetchState, createState, updateState } = nextProps;

    if (!fetchState.inProgress && !createState.inProgress && !updateState.inProgress) {
      const { taxon } = nextProps.details;

      this.setState({ taxon });
    }
  }

  get actions(): ObjectActions<Taxon> {
    return {
      ...this.props.actions,
      close: transitionToLazy('values', {
        taxonomyId: this.props.params.taxonomyId,
        context: this.props.params.context
      }),
      getTitle: (t: Taxon) => get(t.attributes, 'name.v', ''),
      transition: (id: number|string) => transitionTo('value-details', {
        taxonomyId: this.props.params.taxonomyId,
        context: this.props.params.context,
        taxonId: id
      })
    };
  }

  get navLinks(): NavLinks<TaxonParams> {
    return [
      { title: 'Details', to: 'value-details', params: this.props.params },
      { title: 'Products', to: 'value-products', params: this.props.params },
    ];
  }

  get fetchState(): AsyncState {
    const { details, fetchState } = this.props;

    const inProgress = fetchState.inProgress;
    const noError = (!details.taxon && !fetchState.err) || (!schema);
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
  handleObjectUpdate(obj) {
    const { taxon } = this.state;

    if (taxon) {
      const newTaxon = assoc(
        taxon, 'attributes', obj.attributes,
      );

      this.setState({ taxon: newTaxon });
    }
  }

  render() {
    const { details, archiveState, params: { taxonId, context }, children }  = this.props;

    return (
      <ObjectPageDeux
        context={context}
        layout={layout}
        schema={schema}
        identifier={taxonId}
        object={this.state.taxon}
        objectType="value"
        internalObjectType="taxon"
        originalObject={details.taxon}
        actions={this.actions}
        onUpdateObject={this.handleObjectUpdate}
        fetchState={this.fetchState}
        saveState={this.saveState}
        archiveState={archiveState}
        navLinks={this.navLinks}
      >
        {children}
      </ObjectPageDeux>
    );
  }
}

const mapState = state => ({
  details: state.taxons.details,
  fetchState: get(state.asyncActions, 'fetchTaxon', {}),
  createState: get(state.asyncActions, 'createTaxon', {}),
  updateState: get(state.asyncActions, 'updateTaxon', {}),
  archiveState: get(state.asyncActions, 'archiveTaxon', {}),
});

const mapActions = (dispatch, props) => ({
  actions: {
    ...bindActionCreators(taxonsActions, dispatch),
    create: bindActionCreators(taxonsActions.create(props.params.taxonomyId), dispatch)
  }
});

export default connect(mapState, mapActions)(TaxonPage);

