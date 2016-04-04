
// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SelectableSearchList } from '../list-page';

// redux
import { actions } from '../../modules/promotions/list';
import { actions as bulkActions } from '../../modules/promotions/bulk';

// styles
import styles from './promotions.css';


const mapStateToProps = (state) => {
  return {
    list: state.promotions.list,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

const tableColumns = [];

@connect(mapStateToProps, mapDispatchToProps)
export default class Promotions extends Component {

  renderRow() {
    return null;
  }

  bulkActions() {
    return [];
  }

  renderDetail() {
    return null;
  }

  render() {
    const {list, actions} = this.props;

    const entity = 'promotion';
    const module = `${entity}s`;

    return (
      <div styleName="promotions">
        <BulkMessages
          storePath={`${module}.bulk`}
          module={module}
          entity={entity}
          renderDetail={this.renderDetail} />
        <BulkActions
          module={module}
          entity={entity}
          actions={this.bulkActions()}>
          <SelectableSearchList
            emptyMessage="No promotions found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
          />
        </BulkActions>
      </div>
    );
  }
}
