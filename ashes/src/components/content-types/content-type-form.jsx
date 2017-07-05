
/* @flow weak */

import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import classNames from 'classnames';

import styles from '../object-page/object-details.css';

import ObjectDetails from '../object-page/object-details';
import Modal from './modal';
import Form from './form';
import { FormField } from '../forms';
import RadioButton from 'components/core/radio-button';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';

import ContentBox from 'components/content-box/content-box';
import { Button } from 'components/core/button';

import {
  addContentTypeObject,
  updateContentTypeObject,
  removeContentTypeObject
} from 'paragons/content-type';
import { setObjectAttr, omitObjectAttr } from 'paragons/object';
import { customerGroups } from 'paragons/object-types';
const layout = require('./layout.json');

export default class ContentTypeForm extends ObjectDetails {

  layout = layout;

  state = {
    tabs: {},
    sections: {},
    properties: {},
  }

  renderApplyType() {
    const promotion = this.props.object;
    return (
      <FormField
        ref="applyTypeField"
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="autoApplyRadio"
            onChange={this.handleApplyTypeChange}
            name="auto"
            checked={promotion.applyType === 'auto'}>
            <label htmlFor="autoApplyRadio" styleName="field-label">Promotion is automatically applied</label>
          </RadioButton>
          <RadioButton id="couponCodeRadio"
            onChange={this.handleApplyTypeChange}
            name="coupon"
            checked={promotion.applyType === 'coupon'}>
            <label htmlFor="couponCodeRadio" styleName="field-label">Promotion requires a coupon code</label>
          </RadioButton>
        </div>
      </FormField>
    );
  }


  get usageRules() {
    return _.get(this.props, 'object.attributes.usageRules.v', {});
  }

  renderUsageRules() {
    const isExclusive = _.get(this.usageRules, 'isExclusive');
    return (
      <FormField
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="isExlusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="true"
            checked={isExclusive === true}>
            <label htmlFor="isExlusiveRadio">Promotion is exclusive</label>
          </RadioButton>
          <RadioButton id="notExclusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="false"
            checked={isExclusive === false}>
            <label htmlFor="notExclusiveRadio">Promotion can be used with other promotions</label>
          </RadioButton>
        </div>
      </FormField>
    );
  }

  @autobind
  handleQualifierChange(qualifier: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'qualifier', qualifier
    );

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleOfferChange(offer: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'offer', offer
    );

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleApplyTypeChange({target}: Object) {
    const value = target.getAttribute('name');
    const newPromotion = assoc(this.props.object, 'applyType', value);

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleUsageRulesChange({target}: Object) {
    const value = (target.getAttribute('name') === 'true');
    const newPromotion = setObjectAttr(this.props.object, 'usageRules', {
      t: 'PromoUsageRules',
      v: {
        'isExclusive': value
      }
    });

    this.props.onUpdateObject(newPromotion);
  }

  renderState(): ?Element<*> {
    return super.renderState();
  }

  renderDiscounts() {
    let discountChilds = [];
    const discounts = _.get(this.props.object, 'discounts', []);
    discounts.map((disc,index) => {
      discountChilds.push(<div styleName="sub-title">Qualifier</div>),
      discountChilds.push(<DiscountAttrs
        blockId={'promo-qualifier-block-'+index}
        dropdownId={'promo-qualifier-dd-'+index}
        discount={disc}
        attr="qualifier"
        descriptions={qualifiers}
        onChange={this.handleQualifierChange}
      />);
      discountChilds.push(<div styleName="sub-title">Offer</div>),
      discountChilds.push(<DiscountAttrs
        blockId={'promo-offer-block-'+index}
        dropdownId={'promo-offer-dd-'+index}
        discount={disc}
        attr="offer"
        descriptions={offers}
        onChange={this.handleOfferChange}
      />);
    });
    return (
      <div>
        {discountChilds}
      </div>
    );
  }

  @autobind
  handleQualifyAllChange(isAllQualify) {
    const promotion = this.props.object;
    let newPromotion;
    if (isAllQualify) {
      newPromotion = omitObjectAttr(promotion, 'customerGroupIds');
    } else {
      newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups([]));
    }
    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleQualifierGroupChange(ids){
    const promotion = this.props.object;
    const newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups(ids));
    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  setIsVisible(key, value) {
    return id => {
      this.setState({
        [key]: {
          ...this.state[key],
          showModal: value,
          id,
        }
      });
    };
  }

  @autobind
  onSave(key, id) {
    return attributes => {
      const { object: contentType } = this.props;
      if (id > 0) {
        this.props.onUpdateObject(updateContentTypeObject(contentType, key, id, attributes));
        return id;
      } else {
        const object = addContentTypeObject(contentType, key, attributes);
        this.props.onUpdateObject(object);
        return _.last(object[key].allIds);
      }
    };
  }

  @autobind
  onDelete(key, id) {
    return () => {
      const { object: contentType } = this.props;
      this.props.onUpdateObject(removeContentTypeObject(contentType, key, id));
    };
  }

  @autobind
  onCancel(key) {
    return this.setIsVisible(key, false);
  }

  formData(key: string) {
    const schemes = {
      tabs: {
        fieldsToRender: ['title'],
        schema: {
          "type": "object",
          "required": [
            "title"
          ],
          "properties": {
            "title": {
              "type": "string",
              "minLength": 1
            },
            "slug": {
              "type": "string",
              "minLength": 1
            },
            "custom-properties": {
              "title": "Custom Properties can be added to this section",
              "type": "boolean"
            }
          }
        }
      },
      sections: {
        fieldsToRender: ['title', 'slug', 'custom-properties'],
        schema: {
          "type": "object",
          "required": [
            "title"
          ],
          "properties": {
            "title": {
              "type": "string",
              "minLength": 1
            },
            "slug": {
              "type": "string",
              "minLength": 1
            },
            "custom-properties": {
              "title": "Custom Properties can be added to this section",
              "type": "boolean"
            }
          }
        }
      },
      properties: {
        fieldsToRender: ['title', 'slug'],
        schema: {
          "type": "object",
          "required": [
            "title"
          ],
          "properties": {
            "title": {
              "type": "string",
              "minLength": 1
            },
            "slug": {
              "type": "string",
              "minLength": 1
            },
            "custom-properties": {
              "title": "Custom Properties can be added to this section",
              "type": "boolean"
            }
          }
        }
      }
    };

    return _.get(schemes, key, {});
  }

  modal({ key, title }): Element<*> {
    const formData = this.formData(key);
    const { object: contentType } = this.props;
    const { id, showModal } = this.state[key];

    return (
      <Modal
        title={`New ${title}`}
        schema={formData.schema}
        object={_.get(contentType[key].byId[id], 'attributes', {})}
        fieldsToRender={formData.fieldsToRender}
        isVisible={showModal}
        onCancel={this.onCancel(key)}
        onSave={this.onSave(key, id)}
      />
    );
  }

  form({ key }): Element<*> {
    const formData = this.formData(key);
    const { object: contentType } = this.props;
    const { id, showModal } = this.state[key];

    if (!showModal) return null;

    return (
      <Form
        schema={formData.schema}
        object={_.get(contentType[key].byId[id], 'attributes', {})}
        fieldsToRender={formData.fieldsToRender}
        onCancel={this.onCancel(key)}
        onSave={this.onSave(key, id)}
      />
    );
  }


  column({ key, title, children, footer, emptyBody }): Element<*> {
    const isEmpty = _.isEmpty(children);

    const bodyClassName = classNames(
      styles['column-body'],
      {[styles['column-body-empty']]: isEmpty}
    );

    return (
      <ContentBox
        className={styles['column']}
        bodyClassName={bodyClassName}
        title={title}
        actionBlock={this.actions}
        footer={(
          <div styleName="column-footer">
            {footer}
          </div>
        )}
        indentContent={false}
      >
        {children}
        {isEmpty && emptyBody}
        {key === 'properties' ? null : this.modal({ key, title })}
      </ContentBox>
    );
  }

  renderColumns(): Element<*> {
    const { object: contentType } = this.props;
    return (
      <div styleName="columns">
        {this.column(
          {
            key: 'tabs',
            title: 'Tab',
            emptyBody: (
              <span>
                Add a tab!
              </span>
            ),
            footer: (
              <Button
                icon="add"
                onClick={this.setIsVisible('tabs', true)}
              >
                Tab
              </Button>
            ),
            children: _.map(contentType.tabs.byId, (tab) => <Button>{tab.attributes.title.v}</Button>)
          }
        )}
        {this.column(
          {
            key: 'sections',
            title: 'Section',
            emptyBody: (
              <span>
                Add a section!
              </span>
            ),
            footer: (
              <Button
                icon="add"
                onClick={this.setIsVisible('sections', true)}
              >
                Section
              </Button>
            ),
            children: _.map(contentType.sections.byId, (section, id) => (
              <div>
                {section.attributes.title.v}
                <Button icon="edit" onClick={() => this.setIsVisible('sections', true)(id)}>Edit</Button>
              </div>
            ))
          }
        )}
        {this.column(
          {
            key: 'properties',
            title: 'Properties',
            emptyBody: (
              <span>
                Add a property!
              </span>
            ),
            footer: (
              <Button
                icon="add"
                onClick={() => {
                  const id = this.onSave('properties')({ title: { t: 'string', v: '' } });
                  this.setIsVisible('properties', true)(id);
                }}
              >
                Property
              </Button>
            ),
            children: _.map(this.props.object.properties.byId, (property, id) => (
              <Button
                onClick={() => {
                  this.setIsVisible('properties', true)(id);
                }}
              >
                {_.get(property.attributes, 'title.v') || 'New property'}
              </Button>
            ))
          }
        )}
        {this.column(
          {
            key: 'properties',
            title: 'Property Settings',
            footer: this.state.properties.showModal ? (
              <Button
                onClick={_.compose(
                  this.onDelete('properties', this.state.properties.id),
                  this.setIsVisible('properties', false)
                )}
              >
                Delete
              </Button>
            ) : null,
            children: this.form(
              {
                key: 'properties',
              }
            )
          }
        )}
      </div>
    );
  }
}
