// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import styles from './plugins-list.css';

import { PageTitle } from 'components/section-title';
import Table from '../table/table';
import Link from '../link/link';

import * as PluginsActions from 'modules/plugins';

type Props = {
  plugins: Array<Object>,
  fetchPlugins: () => Promise<*>,
  isLoading: boolean,
}

function mapStateToProps(state) {
  return {
    plugins: state.plugins.list,
    isLoading: _.get(state.asyncActions, 'fetchPlugins.inProgress', true),
  };
}

const tableColumns: Array<Object> = [
  { field: 'name', text: 'Plugin',
    render: name => <Link to="plugin" params={{name: name}}>{name}</Link>
  },
  { field: 'description', text: 'Description' },
  { field: 'version', text: 'Version' },
  { field: 'createdAt', text: 'Date/Time Created', type: 'datetime' },
];

class PluginsList extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchPlugins();
  }

  get pluginsTableData(): Object {
    const { plugins } = this.props;
    return {
      rows: plugins,
      total: plugins.length,
      from: 0
    };
  }

  render() {
    return (
      <div>
        <PageTitle title="Plugins" />
        <div styleName="block">
          <Table
            columns={tableColumns}
            data={this.pluginsTableData}
            isLoading={this.props.isLoading}
            emptyMessage="There are no plugins installed"
          />
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, PluginsActions)(PluginsList);
