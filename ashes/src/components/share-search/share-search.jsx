/* flow */

/** Libs */
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { numberize } from 'lib/text-utils';

/** Component */
import Modal from 'components/core/modal';

import { PrimaryButton } from 'components/core/button';
import Spinner from 'components/core/spinner';
import AdminsTypeahead from '../users-typeahead/admins-typeahead';
import Alert from 'components/core/alert';

// styles
import s from './share-search.css';

const mapStateToProps = (state, props) => {
  const search = _.invoke(state, `${props.entity}.currentSearch`);

  return {
    search,
    shares: _.get(search, 'shares', {}),
  };
};


class ShareSearch extends Component {
  static propTypes = {
    search: PropTypes.object.isRequired,
    shares: PropTypes.object.isRequired,
    title: PropTypes.string.isRequired,
    isVisible: PropTypes.bool.isRequired,
    maxUsers: PropTypes.number,
    fetchAssociations: PropTypes.func.isRequired,
    associateSearch: PropTypes.func.isRequired,
    dissociateSearch: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired,
  };

  static defaultProps = {
    maxUsers: 3,
  };

  state = {
    firstLoad: true,
    numberUpdatedUsers: false,
    failed: false,
    selected: [],
  };

  componentWillReceiveProps(nextProps) {
    if (nextProps.search.code && this.props.search.code != nextProps.search.code) {
      this.setState({
        firstLoad: true,
        numberUpdatedUsers: 0,
      });

      return this.props.fetchAssociations(nextProps.search);
    }

    const nextState = {};

    if (!this.props.isVisible && nextProps.isVisible) {
      nextState.numberUpdatedUsers = 0;
    }

    const numberUpdatedUsers = nextProps.shares.associations.length - this.props.shares.associations.length;

    if (numberUpdatedUsers && !this.state.firstLoad) {
      nextState.numberUpdatedUsers = numberUpdatedUsers;
    }

    if (this.props.shares.isFetchingAssociations && !nextProps.shares.isFetchingAssociations) {
      nextState.firstLoad = false;
    }

    this.setState(nextState);
  }

  get title() {
    return <span>Share Search: <strong>{this.props.title}</strong></span>;
  }

  get alert() {
    const { numberUpdatedUsers, failed } = this.state;
    if (numberUpdatedUsers || failed) {
      let label = '';
      const absCount = Math.abs(numberUpdatedUsers);
      if (failed) {
        label = `Failed updating data.`;
      } else if (numberUpdatedUsers > 0) {
        label = `Search was successfully shared with ${absCount} ${numberize('user', absCount)}.`;
      } else {
        label = `Search was successfully unshared from ${absCount} ${numberize('user', absCount)}.`;
      }

      return (
        <Alert
          className={s.alert}
          type={numberUpdatedUsers ? Alert.SUCCESS : Alert.ERROR}
          closeAction={this.setState.bind(this, { numberUpdatedUsers: 0 }, null)}
        >
          <span>{label}</span>
        </Alert>
      );
    }
  }

  get associationsList() {
    const { shares: { isFetchingAssociations = false, associations = [] }, search } = this.props;

    if (isFetchingAssociations) {
      return <Spinner size="s" />;
    }

    const associationsNumber = associations.length ? associations.length : '...';

    return (
      <div>
        <p>Shared with <strong>{associationsNumber}</strong> users:</p>
        <ul className="fc-share-search__associations-list">
          {associations.map(item => {
            const isOwner = item.id === search.storeAdminId;
            const closeHandler = isOwner ? _.noop : this.props.dissociateSearch.bind(null, search, item.id);
            const closeButtonClass = isOwner ? '_disabled' : '';

            return (
              <li key={item.id}>
                <span>{item.name}</span>
                <span className="fc-share-search__associations-owner">{isOwner ? 'Owner' : ''}</span>
                <span>{item.email}</span>
                <span>
                  <a className={closeButtonClass} onClick={closeHandler}>
                    &times;
                  </a>
                </span>
              </li>
            );
          })}
        </ul>
      </div>
    );
  }

  @autobind
  handleSelectUsers(users: Array<TUser>) {
    this.setState({
      selected: users,
    });
  }

  render() {
    const { state } = this;
    const { search, shares, maxUsers } = this.props;
    const { isUpdatingAssociations = false, associations = [] } = shares;

    const associationsById = _.keyBy(associations, 'id');

    return (
      <Modal
        title={this.title}
        isVisible={this.props.isVisible}
        onClose={this.props.onClose}
      >
        {this.alert}
        <AdminsTypeahead
          className={s.typeahead}
          hideOnBlur
          label="Invite Users"
          onSelect={this.handleSelectUsers}
          maxUsers={maxUsers}
          mapAdmins={admins => _.filter(admins, admin => !(admin.id in associationsById))}
        />
        <div className="fc-share-search__controls">
          <PrimaryButton
            className="fc-align-right"
            isLoading={isUpdatingAssociations}
            onClick={this.props.associateSearch.bind(null, search, state.selected)}
            disabled={state.selected.length === 0}
          >
            Share
          </PrimaryButton>
        </div>

        <div className="fc-share-search__associations">
          {this.associationsList}
        </div>
      </Modal>
    );
  }
}

export default connect(mapStateToProps)(ShareSearch);
