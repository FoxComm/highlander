// @flow weak

import _ from 'lodash';
import React, { Component, Element } from 'react';
import styles from './object-details.css';
import invariant from 'invariant';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import { flow, filter } from 'lodash/fp';
import { expandRefs } from 'lib/object-schema';
import { addKeys } from 'lib/react-utils';

// components
import ContentBox from '../content-box/content-box';
import ObjectFormInner from '../object-form/object-form-inner';
import ObjectScheduler from '../object-scheduler/object-scheduler';
import { Form } from '../forms';
import Tags from '../tags/tags';
import ParticipantsPanel from '../participants';

import type { ObjectView } from 'paragons/object';

type Layout = {
  content: Array<Object>,
  aside: Array<Object>,
}

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

export type DetailsProps = {
  title: string, // object title
  plural: string,
  object: ObjectView,
  schema: Object,
  isNew: boolean,
  onUpdateObject: (object: ObjectView) => void,
  // for product form
  // somehow flow don't understand props declarations in extended classes
  // in case of existing props declarations in base class
  onSetSkuProperty: (code: string, field: string, value: any) => void,
  onSetSkuProperties: (code: string, toUpdate: Array<Array<any>>) => void,
}

export default class ObjectDetails extends Component {
  layout: Layout;

  get schema() {
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
    const renderName = `render${_.upperFirst(_.camelCase(description.type))}`;
    // $FlowFixMe: we don't need indexable signature here
    invariant(this[renderName], `There is no method for render ${description.type}.`);
    return this[renderName](description, section);
  }

  renderSection(name: string): Element {
    const section = this.layout[name];
    return addKeys(name, section.map(desc => this.renderNode(desc, section)));
  }

  get aside() {
    if (this.layout.aside != null) {
      return (
        <div styleName="aside">
          {this.renderSection('aside')}
        </div>
      );
    }
  }

  render() {
    const mainStyleName = this.layout.aside != null ? 'main' : 'full-page';

    return (
      <Form ref="form" styleName="object-details">
        <div styleName={mainStyleName}>
          {this.renderSection('main')}
        </div>
        {this.aside}
      </Form>
    );
  }
}

