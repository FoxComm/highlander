/* @flow */

// libs
import React, {Component, Element} from 'react';
import {autobind} from 'core-decorators';

// components
import { Button } from '../common/buttons';
import ArchiveConfirmation from './archive-confirmation';

type Props = {
  title: string,
  type: string,
  archive: Function,
  archiveState: AsyncStatus,
  clearArchiveErrors: () => Promise,
};

type State = {
  archiveConfirmation: boolean;
};

class ArchiveActions extends Component {
  props: Props;

  state: State = {
    archiveConfirmation: false,
  };

  componentDidMount() {
    this.props.clearArchiveErrors();
  }

  @autobind
  showArchiveConfirmation() {
    this.setState({
      archiveConfirmation: true,
    });
  }

  @autobind
  closeConfirmation() {
    this.props.clearArchiveErrors();
    this.setState({
      archiveConfirmation: false,
    });
  }

  @autobind
  archive() {
    this.props.clearArchiveErrors();
    this.props.archive();
  }

  render():Element {
    return (
      <div className="fc-archive-actions">
        <Button
          type="button"
          onClick={this.showArchiveConfirmation}>
          Archive {this.props.type}
        </Button>
        <ArchiveConfirmation
          isVisible={this.state.archiveConfirmation}
          closeConfirmation={this.closeConfirmation}
          { ...this.props }
          archive={this.archive}
        />
      </div>
    );
  }
}

export default ArchiveActions;
