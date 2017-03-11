// @flow

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
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

type Layout = {
  content: Array<Object>,
  aside: Array<Object>,
};

type Fields = {
  canAddProperty: boolean,
  value: Array<string>,
  includeRest: boolean,
  omit: Array<string>,
}

type NodeDesc = {
  type: string,
  title?: string,
  fields?: Fields,
  renderer?: string,
  content?: Array<NodeDesc>,
}

type Props = {
  layout: Layout,
  title: string,
  plural: string,
  object: ObjectView,
  schema: ObjectSchema,
  onUpdateObject: (object: ObjectView) => void,
  renderers: { [key: string]: (desc: NodeDesc) => ?Element<*> }
};

export default class ObjectDetailsDeux extends Component {
  props: Props;

  get schema(): Object {
    return expandRefs(this.props.schema);
  }

  get attributes(): Attributes {
    return _.get(this.props, 'object.attributes', {});
  }

  checkValidity(): boolean {
    return this.refs.form.checkValidity();
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
        title={this.props.title}
      />
    );
  }

  renderWatchers() {
    const { object, plural } = this.props;

    if (object.id) {
      return <ParticipantsPanel entity={{entityId: object.id, entityType: plural}} />;
    }
  }

  renderId() {
    if (!this.props.object.id) {
      return null;
    }

    return (
      <div>
        <div className="fc-object-form__field-label">
          <label>ID</label>
        </div>
        <div className="fc-object-form__field">
          {this.props.object.id}
        </div>
      </div>
    );
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
        {this.renderId()}
        {children}
      </ContentBox>
    );
  }

  renderNode(description: NodeDesc, section: Array<NodeDesc>) {
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
        const renderName = description.type;
        invariant(this.props.renderers[renderName], `There is no method for render ${description.type}.`);
        return this.props.renderers[renderName](description, section);
    }
  }

  renderSection(name: string) {
    const { layout } = this.props;
    const section = layout[name];

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
