// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import styles from './plugin.css';

import { PageTitle } from 'components/section-title';
import ObjectFormFlat from 'components/object-form/object-form-flat';
import SaveCancel from 'components/common/save-cancel';
import WaitAnimation from 'components/common/wait-animation';

import * as PluginsActions from 'modules/plugins';
import type { UpdateSettingsPayload, SettingDef } from 'modules/plugins';

type Props = {
  fetchSettings: (name: string) => Promise,
  updateSettings: (name: string, payload: UpdateSettingsPayload) => Promise,
  params: {
    name: string,
  },
  settings: Object,
  schema: Object,
  isFetching: boolean,
}

const pluginName = (props: Props): string => {
  return props.params.name;
};

function guessType(value) {
  const typeOf = typeof value;
  switch (typeOf) {
    case 'string':
    case 'number':
      return typeOf;
    case 'boolean':
      return 'bool';
    default:
      return 'string';
  }
}

function attributesFromSettings(settingsWithSchema: Object): Attributes {
  const settings: Object = settingsWithSchema.settings;
  const schema: Object = settingsWithSchema.schema;

  return _.reduce(schema, (acc:Attributes, property: SettingDef) => {
      const value = settings[property.name];
      acc[property.name] = {
        t: guessType(value),
        v: value
      };
    return acc;
  }, {});
}

function mapStateToProps(state) {
  return {
    settings: state.plugins.settings,
    schema: state.plugins.schema,
    isFetching: _.get(state.asyncActions, 'fetchPluginSettings.inProgress', null),
  };
}

type State = {
  settings: Object,
  schema: Object,
}

class Plugin extends Component {
  props: Props;

  state: State = {
    settings: {},
    schema: {},
  };

  componentDidMount() {
    this.props.fetchSettings(pluginName(this.props));
  }

  componentWillReceiveProps(nextProps: Props) {
    if (pluginName(nextProps) != this.pluginName) {
      this.props.fetchSettings(pluginName(nextProps));
    }
    if (!_.isEqual(nextProps.settings, this.state.settings) || !_.isEqual(nextProps.schema, this.state.schema)) {
      this.setState({
        settings: nextProps.settings,
        schema: nextProps.schema
      });
    }
  }

  get pluginName(): string {
    return pluginName(this.props);
  }

  get attributes(): Attributes {
    return attributesFromSettings(this.state);
  }

  @autobind
  handleChange(attributes: Object) {
    this.setState({
      settings: attributes,
    });
  }

  @autobind
  handleSave() {
    this.props.updateSettings(this.pluginName, {
      settings: this.state.settings,
    });
  }

  get content(): Element {
    if (this.props.isFetching !== false) {
      return <WaitAnimation/>;
    } else {
      return (
        <div>
          <ObjectFormFlat
            title={this.pluginName}
            attributes={this.attributes}
            onChange={this.handleChange}
          />
          <SaveCancel
            cancelTo="plugins"
            onSave={this.handleSave}
          />
        </div>
      );
    }
  }


  render() {
    const title = `Plugin ${this.pluginName}`;
    return (
      <div>
        <PageTitle title={title} />
        <div styleName="content">
          {this.content}
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, PluginsActions)(Plugin);
