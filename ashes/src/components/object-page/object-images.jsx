/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';

// components
import Images from '../images/images';

export function connectImages(namespace, actions) {
  const plural = `${namespace}s`;

  const mapStateToProps = (state) => ({
    namespace,
    albums: _.get(state, [plural, 'images', 'albums'], []),
    isLoading: _.get(state, ['asyncActions', `${plural}FetchAlbums`, 'inProgress'], true),
    addAlbumInProgress: _.get(state, ['asyncActions', `${plural}AddAlbum`, 'inProgress'], false),
    editAlbumInProgress: _.get(state, ['asyncActions', `${plural}EditAlbum`, 'inProgress'], false),
    uploadImagesInProgress: _.get(state, ['asyncActions', `${plural}UploadImages`, 'inProgress'], false),
  });

  return ImagesPage => {
    return connect(mapStateToProps, actions)(ImagesPage);
  };
}

export default class ImagesPage extends Component {

  get entityId(): string {
    return this.props.entity.entityId;
  }

  get contextName(): string {
    return this.props.contextName;
  }

  componentDidMount(): void {
    this.props.fetchAlbums(this.contextName, this.entityId);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.albums != this.props.albums) {
      this.props.syncEntity({
        albums: nextProps.albums,
      });
    }
  }

  render() {
    const { params, ...rest } = this.props;

    return (
      <Images {...rest} entityId={this.entityId} context={this.contextName} />
    );
  }
}

