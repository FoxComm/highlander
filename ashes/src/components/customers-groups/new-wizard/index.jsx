/* @flow */

import { get, isEmpty, find } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import { fetchTemplates } from 'modules/customer-groups/templates';
import { saveGroupFromTemplate as saveGroup, clearSaveErrors } from 'modules/customer-groups/details/group';

import { Link } from 'components/link';
import WaitAnimation from 'components/common/wait-animation';
import Error from 'components/errors/error';
import Template from '../group-template';

import styles from './styles.css';

type Props = {
  templates: TTemplates;
  fetchFinished: boolean;
  fetchError: Object;
  saveInProgress: boolean;
  saveError: Object;
  fetchTemplates: () => Promise;
  saveGroup: () => Promise;
  clearSaveErrors: () => void;
  push: (location: Object) => void;
};

class NewGroupWizardPage extends Component {
  props: Props;

  componentWillMount() {
    this.props.clearSaveErrors();
  }

  componentDidMount() {
    if (isEmpty(this.props.templates)) {
      this.props.fetchTemplates();
    }
  }

  @autobind
  handleSave(templateId: number) {
    const { templates, saveGroup, push } = this.props;
    const template = find(templates, (template: TTemplate) => template.id === templateId);

    saveGroup(template)
      .then(res => {
        push({ name: 'customer-group', params: { groupId: res.id } });
      })
      .then(this.props.fetchTemplates);
  }

  @autobind
  handleCustomGroup(type: string) {
    return () => {
      this.props.push({ name: 'new-custom-group', params: { type }});
    };
  }

  render() {
    const { templates, fetchFinished, fetchError } = this.props;

    if (fetchError) {
      return <Error err={fetchError} />;
    }

    if (!fetchFinished) {
      return <div><WaitAnimation /></div>;
    }

    return (
      <div className={styles.wizard}>
        <Template name="Dynamic Group" onClick={this.handleCustomGroup('dynamic')} icon="customers" />
        <Template name="Manual Group" onClick={this.handleCustomGroup('manual')} icon="customers" />

        {templates.map((tpl: TTemplate) => (
          <Template id={tpl.id} name={tpl.name} onClick={this.handleSave} key={tpl.id} />
        ))}
      </div>
    );
  }
}

const mapStateToProps = state => ({
  fetchFinished: get(state, 'asyncActions.fetchCustomerGroupsTemplates.finished', false),
  saveInProgress: get(state, 'asyncActions.saveCustomerGroup.inProgress', false),
  saveError: get(state, 'asyncActions.saveCustomerGroup.err', false),
  templates: state.customerGroups.templates
});

const mapActions = { fetchTemplates, saveGroup, clearSaveErrors, push };

export default connect(mapStateToProps, mapActions)(NewGroupWizardPage);
