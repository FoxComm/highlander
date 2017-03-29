/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { get, omit } from 'lodash';

// actions
import { fetch } from 'modules/taxonomies/flatList';

// components
import WaitAnimation from 'components/common/wait-animation';

// styles
import styles from './taxonomy.css';

const omitProps = [
  'taxonomies',
  'fetch',
  'createState',
  'updateState',
  'archiveState',
];

function getDisplayName(WrappedComponent) {
  return WrappedComponent.displayName || WrappedComponent.name || 'Component';
}

type Props = {
  taxonomies: Array<TaxonomyResult>,
  fetch: () => Promise<*>,
  fetchState: AsyncState,
  archiveState: AsyncState,
  createState: AsyncState,
  updateState: AsyncState,
};

type Options = {
  showLoader?: boolean,
};

const defaultOptions: Options = {
  showLoader: true,
};

export function withTaxonomies(options: Options = defaultOptions) {
  // TODO: proper type for component argument
  return function (WrappedComponent: any) {
    class Wrapper extends Component {
      props: Props;

      componentDidMount() {
        this.props.fetch();
      }

      componentWillReceiveProps(nextProps: Props) {
        const created = this.props.createState.inProgress && !nextProps.createState.inProgress;
        const updated = this.props.updateState.inProgress && !nextProps.updateState.inProgress;
        const archived = this.props.archiveState.inProgress && !nextProps.archiveState.inProgress;

        if (created || updated || archived) {
          this.props.fetch();
        }
      }

      render() {
        const { taxonomies, fetchState } = this.props;

        if (!taxonomies || fetchState.inProgress && !fetchState.err && options.showLoader) {
          return <WaitAnimation className={styles.waiting} />;
        }

        const props = {
          [get(options, 'taxonomiesField', 'taxonomies')]: this.props.taxonomies,
          [get(options, 'fetchStateField', 'fetchState')]: this.props.fetchState,
          ...omit(this.props, omitProps),
        };

        return <WrappedComponent {...props} />;
      }
    }

    Wrapper.displayName = `WithTaxonomies(${getDisplayName(WrappedComponent)})`;

    const mapState = state => ({
      taxonomies: state.taxonomies.flatList,
      fetchState: get(state.asyncActions, 'fetchTaxonomies', {}),
      createState: get(state.asyncActions, 'createTaxonomy', {}),
      updateState: get(state.asyncActions, 'updateTaxonomy', {}),
      archiveState: get(state.asyncActions, 'archiveTaxonomy', {}),
    });

    return connect(mapState, { fetch })(Wrapper);
  };
}
