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
import * as taxonsActions from 'modules/taxons/details';

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

const schema = {
  'type': 'object',
  'title': 'Taxon',
  '$schema': 'http://json-schema.org/draft-04/schema#',
  'properties': {
    'attributes': {
      'type': 'object',
      'required': [
        'name'
      ],
      'properties': {
        'name': {
          'type': 'string',
          'minLength': 1
        },
        'description': {
          'type': 'string',
          'widget': 'richText'
        }
      },
      'description': 'Taxon attributes itself'
    }
  }
};

class TaxonPage extends React.Component {
  props: any;
  state = { taxon: null };

  componentDidMount () {
    console.log('componentDidMount')
  }

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
    return [{
      title: 'Details',
      to: 'value-details',
      params: this.props.params,
      key: 'value-details-link',
    }];
  }

  get fetchState(): AsyncState {
    const { details, fetchState } = this.props;

    const inProgress = fetchState.inProgress;
    const noError = (!details.taxon && !fetchState.err) || (!schema);
    console.log(!details.taxon);
    console.log(!fetchState.err);
    console.log(!schema);
    console.log(noError);
    console.log(inProgress);
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
    const { taxonomyId, taxonId, context } = this.props.params;
    const { details, archiveState } = this.props;

    const childProps = {
      schema,
      taxon: this.state.taxon,
      onUpdateObject: this.handleObjectUpdate
    };

    const children = React.cloneElement(this.props.children, childProps);

    return (
      <ObjectPageDeux
        actions={this.actions}
        context={context}
        identifier={taxonId}
        fetchState={this.fetchState}
        saveState={this.saveState}
        archiveState={archiveState}
        navLinks={this.navLinks}
        object={this.state.taxon}
        objectType="value"
        originalObject={details.taxon}
        listRoute={{
          name: 'values',
          params: { taxonomyId, context }
        }}
      >
        {children}
      </ObjectPageDeux>
    );
  }
};

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

