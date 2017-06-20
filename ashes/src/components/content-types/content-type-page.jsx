/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { transitionTo } from 'browserHistory';

// actions
import * as ContentTypeActions from 'modules/content-types/details';

class ContentTypePage extends ObjectPage {
  componentDidMount() {
    this.props.actions.clearFetchErrors();
    // this.props.actions.fetchSchema(this.props.namespace, true);
    this.props.actions.fetchSchema('json',
      [
        {
          "name": "content-type",
          "kind": "contentType",
          "schema": {
            "type": "object",
            "title": "Content Type",
            "$schema": "http:\/\/json-schema.org\/draft-04\/schema#",
            "properties": {
              "discounts": {
                "type": "array",
                "items": {
                  "$ref": "#\/definitions\/discount"
                }
              },
              "attributes": {
                "type": "object",
                "required": [
                  "name"
                ],
                "properties": {
                  "name": {
                    "type": "string",
                    "minLength": 1
                  },
                  "activeTo": {
                    "type": [
                      "string",
                      "null"
                    ],
                    "format": "date-time"
                  },
                  "activeFrom": {
                    "type": [
                      "string",
                      "null"
                    ],
                    "format": "date-time"
                  },
                  "customerGroupIds": {
                    "type": [
                      "array",
                      "null"
                    ],
                    "items": {
                      "type": "number"
                    },
                    "uniqueItems": true
                  }
                }
              }
            },
            "definitions": {
              "discount": {
                "type": "object",
                "title": "Discount",
                "$schema": "http:\/\/json-schema.org\/draft-04\/schema#",
                "properties": {
                  "id": {
                    "type": "number"
                  },
                  "attributes": {
                    "type": "object",
                    "properties": {
                      "tags": {
                        "type": "array",
                        "items": {
                          "type": "string"
                        }
                      },
                      "offer": {
                        "type": "object"
                      },
                      "title": {
                        "type": "string"
                      },
                      "qualifier": {
                        "type": "object"
                      },
                      "description": {
                        "type": "string",
                        "widget": "richText"
                      }
                    }
                  }
                }
              }
            }
          }
        }
      ]
    );

    if (this.isNew) {
      this.props.actions.newEntity();
    } else {
      this.fetchEntity()
        .then(({ payload }) => {
          if (isArchived(payload)) this.transitionToList();
        });
    }

    this.props.actions.fetchAmazonStatus()
      .catch(() => {}); // pass
  }

  save(): ?Promise<*> {
    let isNew = this.isNew;
    let willBePromo = super.save();

    if (willBePromo && isNew) {
      willBePromo.then((data) => {
        if (data.applyType === 'coupon') {
          transitionTo('content-type-coupon-new',{promotionId: data.id});
        }
      });
    }

    return willBePromo;
  }

  subNav(): Element<*> {
    return <SubNav applyType={_.get(this.props, 'details.contentType.applyType')} contentTypeId={this.entityId} />;
  }
}

export default connectPage('contentType', ContentTypeActions)(ContentTypePage);
