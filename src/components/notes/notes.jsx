
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import { bindActionCreators } from 'redux';

// components
import ConfirmationDialog from '../modal/confirmation-dialog';
import { PrimaryButton } from '../../components/common/buttons';
import SectionTitle from '../section-title/section-title';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import NoteControls from './controls';
import NoteForm from './form';
import { DateTime } from '../common/datetime';
import SearchableList from '../list-page/searchable-list';

// redux
import * as notesActions from '../../modules/notes';

const entityTitles = {
  rma: 'Return',
  order: 'Order',
  giftCard: 'GiftCard',
  customer: 'Customer'
};

const editingNote = createSelector(
  state => _.get(state.notes, 'currentSearch.results.rows', []),
  state => _.get(state.notes, 'editingNoteId'),
  (notes, editingNoteId) => {
    return _.findWhere(notes, {id: editingNoteId});
  }
);

function mapStateToProps(state) {
  return {
    ...state.notes,
    editingNote: editingNote(state),
  };
}

function mapDispatchToProps(dispatch) {
  return {
    ...bindActionCreators(notesActions, dispatch),
    searchActions: bindActionCreators(notesActions.actions, dispatch),
  };
}

/*eslint "react/prop-types": 0*/

@connect(mapStateToProps, mapDispatchToProps)
export default class Notes extends React.Component {
  static deleteOptions = {
    header: 'Confirm',
    body: 'Are you sure you want to delete this note?',
    confirm: 'Yes',
    cancel: 'No'
  };

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
      entityType: PropTypes.string.isRequired
    })
  };

  componentDidMount() {
    this.props.setCurrentEntity(this.props.entity);
    this.props.searchActions.resetSearch();
    this.props.searchActions.fetch();
  }

  get isCustomerNotes() {
    return this.props.entity.entityType == 'customer';
  }

  get tableColumns() {
    let baseColumns = [
      {field: 'body', text: 'Note'},
      {field: 'author', text: 'Author'}
    ];
    if (this.isCustomerNotes) {
      baseColumns = [
        {field: null, text: 'Transaction'},
        ...baseColumns,
      ];
    }

    return [
      {field: 'createdAt', text: 'Date/Time'},
      ...baseColumns
    ];
  }

  @autobind
  renderNoteRow(row, index, isNew) {
    if (this.props.editingNoteId === row.id) {
      return (
        <TableRow key={`row-${index}`}>
          <TableCell colspan={this.tableColumns.length}>
            <NoteForm
              body={this.props.editingNote && this.props.editingNote.body}
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={data => this.props.editNote(this.props.editingNoteId, data)}
            />
          </TableCell>
        </TableRow>
      );
    } else {
      let transaction = null;
      if (this.isCustomerNotes) {
        let cell = null;
        if (row.referenceType != 'customer') {
          cell = (
            <div>
              <div>{entityTitles[row.referenceType] || row.referenceType}</div>
              <div>{row.referenceId}</div>
            </div>
          );
        }
        transaction = (
          <TableCell>
            {cell}
          </TableCell>
        );
      }
      return (
        <TableRow key={`row-${index}`} isNew={isNew}>
          <TableCell>
            <DateTime value={row.createdAt}/>
          </TableCell>
          {transaction}
          <TableCell>
            {row.body}
          </TableCell>
          <TableCell>
            <NoteControls
              model={row}
              onEditClick={(item) => this.props.startEditingNote(item.id)}
              onDeleteClick={(item) => this.props.startDeletingNote(item.id)}
            />
          </TableCell>
        </TableRow>
      );
    }
  }

  @autobind
  injectAddingForm(rows) {
    // -1 means we want add new note
    if (this.props.editingNoteId === -1) {
      return [
        <TableRow key="row-add">
          <TableCell colspan={this.tableColumns.length}>
            <NoteForm
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={this.props.createNote}
            />
          </TableCell>
        </TableRow>,
        ...rows
      ];
    }
    return rows;
  }

  get controls() {
    return (
      <PrimaryButton icon="add" onClick={this.props.startAddingNote} disabled={!!this.props.isAddingNote } />
    );
  }

  get sectionClassName() {
    const entityType = this.props.entity.entityType;
    return `fc-${entityType}-notes`;
  }

  render() {
    return (
      <div className={this.sectionClassName} >
        <SectionTitle className="fc-grid-gutter fc-notes-section-title" title="Notes">{this.controls}</SectionTitle>
        <SearchableList
          emptyResultMessage="No notes found."
          list={this.props.list}
          renderRow={this.renderNoteRow}
          processRows={this.injectAddingForm}
          tableColumns={this.tableColumns}
          skipInitialFetch={true}
          searchActions={this.props.searchActions}
          searchOptions={{
          singleSearch: true,
          }} />
        <ConfirmationDialog
          {...Notes.deleteOptions}
          isVisible={this.props.noteIdToDelete != null}
          confirmAction={() => this.props.deleteNote(this.props.noteIdToDelete)}
          cancelAction={() => this.props.stopDeletingNote(this.props.noteIdToDelete)}
        />
      </div>
    );
  }
}
