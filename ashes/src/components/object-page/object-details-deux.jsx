// @flow

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import styles from './object-details.css';
import { autobind } from 'core-decorators';
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

// types
import type { ObjectView } from 'paragons/object';

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
  schema: Object,
  onUpdateObject: (object: ObjectView) => void,
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

  renderFields(fields: Fields, section: Array<NodeDesc>): Element {
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

  renderTags(): Element {
    return (
      <Tags
        attributes={this.attributes}
        onChange={this.handleObjectChange}
      />
    );
  }

  renderState(): ?Element {
    return (
      <ObjectScheduler
        attributes={this.attributes}
        onChange={this.handleObjectChange}
        title={this.props.title}
      />
    );
  }

  renderWatchers(): ?Element {
    const { object, plural } = this.props;

    if (object.id) {
      return <ParticipantsPanel entity={{entityId: object.id, entityType: plural}} />;
    }
  }

  renderGroup(group: NodeDesc, section: Array<NodeDesc>): Element {
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

  renderNode(description: NodeDesc, section: Array<NodeDesc>): Element {
    switch (description.type) {
      case 'group':
        return this.renderGroup(description, section);
      case 'state':
        // Do something.
      case 'tags':
        // Do something.
      case 'watchers':
        // Do something.
    }
    return <div></div>;
  }

  renderSection(name: string): Element {
    const { layout } = this.props;
    const section = layout[name];
    return addKeys(name, section.map(desc => this.renderNode(desc, section)));
  }

  render(): Element {
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
