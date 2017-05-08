// @flow

// libs
import _ from 'lodash';
import { pluralize } from 'fleck';
import React, { Component, Element } from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import invariant from 'invariant';
import { assoc } from 'sprout-data';
import { flow, filter } from 'lodash/fp';
import { expandRefs } from 'lib/object-schema';
import { addKeys } from 'lib/react-utils';

// components
import { Form } from 'components/forms';
import ContentBox from 'components/content-box/content-box';
import ObjectFormInner from 'components/object-form/object-form-inner';
import ObjectScheduler from 'components/object-scheduler/object-scheduler';
import Tags from 'components/tags/tags';
import ParticipantsPanel from 'components/participants';

import styles from './object-details.css';

export default class ObjectDetailsDeux extends Component {
  props: ObjectPageChildProps<*>;

  static contextTypes = {
    validationDispatcher: PropTypes.object,
  };

  componentWillMount() {
    this.toggleBindToDispatcher(true);
  }

  componentWillUnmount() {
    this.toggleBindToDispatcher(false);
  }

  get schema(): Object {
    return expandRefs(this.props.schema);
  }

  get attributes(): Attributes {
    return _.get(this.props, 'object.attributes', {});
  }

  toggleBindToDispatcher(bind: boolean) {
    const { validationDispatcher } = this.context;
    if (validationDispatcher) {
      const toggleBind = bind ? validationDispatcher.on : validationDispatcher.removeListener;

      toggleBind.call(validationDispatcher, 'validate', this.checkValidity);
    }
  }

  @autobind
  checkValidity(isValid: Function): boolean {
    return isValid(this.refs.form.checkValidity());
  }

  updateAttributes(attributes: Attributes): Object {
    const { object } = this.props;

    return assoc(object,
      'attributes', attributes
    );
  }

  @autobind
  handleObjectChange(attributes: Attributes) {
    const newObject = this.updateAttributes(attributes);

    this.props.onUpdateObject(newObject);
  }

  calcFieldsToRender(fields: Fields, section: Array<NodeDesc>): Array<string> {
    let result: Array<string> = fields.value || [];

    if (fields.includeRest) {
      const restAttrs: Array<string> = _.reduce(section, (acc: Array<string>, nodeDesc: NodeDesc) => {
        if (nodeDesc.fields) {
          return [...acc, ...nodeDesc.fields.value];
        }
        return acc;
      }, []);
      const toOmit = [...restAttrs, ...(fields.omit || []), ...result];

      const filteredAttributes = flow(
        _.keys,
        filter((attr: string) => !_.includes(toOmit, attr))
      )(this.attributes);

      result = [...result, ...filteredAttributes];
    }

    return result;
  }

  renderFields(fields: Fields, section: Array<NodeDesc>) {
    const fieldsToRender = this.calcFieldsToRender(fields, section);
    const attrsSchema = this.schema.properties.attributes;

    return (
      <ObjectFormInner
        canAddProperty={fields.canAddProperty}
        onChange={this.handleObjectChange}
        fieldsToRender={fieldsToRender}
        attributes={this.attributes}
        schema={attrsSchema}
      />
    );
  }

  renderTags() {
    return (
      <Tags
        attributes={this.attributes}
        onChange={this.handleObjectChange}
      />
    );
  }

  renderState() {
    return (
      <ObjectScheduler
        attributes={this.attributes}
        onChange={this.handleObjectChange}
        title={this.props.objectType}
      />
    );
  }

  renderWatchers() {
    const { object, objectType, internalObjectType } = this.props;

    const entityType = pluralize(internalObjectType || objectType);

    if (object.id) {
      return <ParticipantsPanel entity={{ entityId: object.id, entityType }} />;
    }
  }

  renderGroup(group: NodeDesc, section: Array<NodeDesc>) {
    const { title, fields, renderer, content } = group;

    let children;
    if (content) {
      children = addKeys(title, content.map((desc: NodeDesc) => this.renderNode(desc, section)));
    } else if (fields) {
      children = this.renderFields(fields, section);
    } else {
      children = this.renderNode({
        ...group,
        type: renderer
      }, section);
    }

    return (
      <ContentBox title={title}>
        {children}
      </ContentBox>
    );
  }

  renderNode(description: NodeDesc, section: Array<NodeDesc>) {
    const renderName = description.type;

    switch (description.type) {
      case 'group':
        return this.renderGroup(description, section);
      case 'fields':
        return this.renderFields(_.get(description, 'fields'), section);
      case 'state':
        return this.renderState();
      case 'tags':
        return this.renderTags();
      case 'watchers':
        return this.renderWatchers();
      default:
        invariant(this.props.renderers,
          `There are no renderers provided. Can not find method to render custom field ${description.type}.`);
        invariant(this.props.renderers[renderName], `There is no method for render ${description.type}.`);
        return this.props.renderers[renderName](description, section);
    }
  }

  renderSection(name: string) {
    const section = this.props.layout[name];

    return addKeys(name, section.map(desc => this.renderNode(desc, section)));
  }

  render() {
    return (
      <Form ref="form" styleName="object-details">
        <div styleName="main">
          {this.renderSection('main')}
        </div>
        <div styleName="aside">
          {this.renderSection('aside')}
        </div>
      </Form>
    );
  }
}
