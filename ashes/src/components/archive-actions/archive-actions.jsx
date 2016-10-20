/* @flow */

// libs
import React, {Component, Element} from 'react';
import {autobind} from 'core-decorators';

// components
import { Button } from '../common/buttons';
import ArchveConfirmation from './archive-confirmation';

type Props = {
  title: string,
  type: string,
  archive: Function,
};

type State = {
  archiveConfirmation: boolean;
};

class ArchiveActions extends Component {
  props: Props;

  state: State = {
    archiveConfirmation: false,
  };

  @autobind
  showArchiveConfirmation() {
    this.setState({
      archiveConfirmation: true,
    });
  }

  @autobind
  closeConfirmation() {
    this.setState({
      archiveConfirmation: false,
    });
  }

  render():Element {
    return (
      <div className="fc-archive-actions">
        <Button
          type="button"
          onClick={this.showArchiveConfirmation}>
          Archive {this.props.type}
        </Button>
        <ArchveConfirmation
          isVisible={this.state.archiveConfirmation}
          closeConfirmation={this.closeConfirmation}
          { ...this.props } />
      </div>
    );
  }
}

export default ArchiveActions;
