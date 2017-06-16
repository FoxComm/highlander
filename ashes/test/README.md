# Writing Tests

We have two types of the tests, for now.

* Acceptance tests, for testing components.
* Unit tests, for testing redux modules and other stuff.

## Global API for tests

#### yield renderIntoDocument(markup, appendToDocument = false)

Renders markup to DOM.

If second parameter is `true` rendered markup will be appended to document,
so you can get some elements via calling `document.getElementById(...)` for example.
Defaults to `false`.

* *result.container*: `<DOM Node>` container where was rendered document
* *result.unmount*: `<function>` method for unmount rendered markup from DOM
* *result.container.unmount*: `<function>` same as `result.unmount`
* *result.instance*: `<Rect.Component>` instance of rendered component

#### shallowRender(markup)

Shallow renders markup to tree structure. Rendered component must be statefull.
(Stateless components you can render just calling them.)

* *result.instance*: `<React.Component>` instance of rendered component
* *result.unmount*: `<function>` method for unmount rendered markup
* *result.type*: `<string>` type of rendered component
* *result.props*: `<object>` props of rendered component

#### requireComponent(path)

Support function for require files under `src/components/` path.

#### createContainer(tagName = 'div', attachToDom = false)

Creates the container for rendering some markup in DOM.
Usually you don't need this method.

* *result.unmount*: `<function>` method for unmount rendered markup from DOM

#### importSource(path, variablesToExport = [])

Imports any lib under `src/` path and exports private variables that you have defined.

#### requireSource(path)

Just helper for imports any lib under `src/` path, it uses regular `require` for import.
For import non-exported attributes use `importSource` instead

## Components

There are several approaches for testing React components,
which depend on how component declared and how component rendered.

Accordingly, there are several npm packages for testing components
rendered in DOM and rendering just in some internal tree representation of component markup.

[react-test-renderer](https://www.npmjs.com/package/react-test-renderer),
React renderers that can be used for testing purposes.

```es6
import ShallowRenderer from 'react-test-renderer/shallow';
```

[ReactTestUtils](https://facebook.github.io/react/docs/test-utils.html),
support library for testing rendered components in DOM.

```es6
import ReactTestUtils from 'react-dom/test-utils';
```

### Shallow render components

Stateless components are rendered to the tree structure via calling them to defined props.
And you can test what happened via looking/asserting to that tree structure.

Example:

```es6
const button = PrimaryButton({icon: 'add'});
expect(button.type).to.equal('SimpleButton');
expect(button.props.className).to.contain('fc-btn-primary');
expect(button.props.children[0].props.className).to.contain('icon-add');
```

Also you can shallow render common component via global function `shallowRender`:

```es6
const contentBox = shallowRender(
  <ContentBox title={ title } className="" />
);
```

You can call instance methods via `instance` property:

```es6
contentBox.instance.tick();
expect(contentBox.props.className).to.equal('fc-content-box');
```

In both cases you can use a helpful [unexpected-react-shallow](https://github.com/bruderstein/unexpected-react-shallow#assertions) library
for testing shallow rendered components.

```es6
  expect(contentBox, 'to contain',
    <div className="fc-col-md-2-3 fc-title">{title}</div>
  );
```

Or you can use [`ShallowTestUtils`](https://github.com/sheepsteak/react-shallow-testutils#react-shallow-testutils) utility
for retrieve some elements based on defined criteria.

```es6
ShallowTestUtils.findWithClass(dialog, 'fc-modal');
```

Tip: `unexpected-react-shallow` matches components only if passed properties are strictly matched, i.e.
```es6
<div className="fc-title">{title}</div>
```
don't match element like
```es6
<div className="fc-col-md-2-3 fc-title">{title}</div>
```
for example.

So, sometimes it's more convenient to use the `ShallowTestUtils` utility rather than `unexpected-react-shallow`.

One more tip: `ShallowTestUtils.findWithClass` and other methods that select only single element always raise exception
if element not found, so you don't need for constructions like `except(findWithClass(...)).to.be.ok`.

### Render components in DOM

For render commmon components to DOM use `yield renderIntoDocument(jsx, shouldAppendToDocument)` construction.

```es6
const formfield = yield renderIntoDocument(
  <FormField maxLength={5} validator='ascii' label="Lorem Ipsum">
    <input type="text" value="Кошку ела собака"/>
  </FormField>,
  true
);
```

In this case you can use `TestUtils` for retrieving rendered elements from rendered component, e.g.:

```es6
const inputNode = TestUtils.findRenderedDOMComponentWithTag(formfield, 'input');
```

Tip: `TestUtils.findRenderedDOMComponentWithTag` already returns dom element if you are looking for native element.
I.e. you don't need to call `findDOMNode` to `findRenderedDOMComponentWithTag` result.

Or you can use `container` property if needed.

```es6
assert(formfield.container.querySelector('input').value).to.not.be.empty
```

### Render stateless components in DOM

For render stateless components to DOM use same approach for render common components, but there is one exception -
you should always wrap stateless component into `<div>`:

```es6
const { container } = yield renderIntoDocument(
  <div><LineItems {...defaultProps} /></div>
);
```

Also you can't use TestUtils or ShallowTestUtils in this case,
so use native DOM methods on container for that, `querySelector` for example.

```es6
expect(container.querySelector('.fc-modal')).to.not.equal(null);
```

## Redux modules

There are two things for testing in redux modules - async actions and reducer.

Use `importModule` global function to get redux module if you needed private actions for testing purposes.
For example.

```es6
const {reducer, ...actions} = importModule('notes.js', [
  'receiveNotes',
  'updateNotes',
  'noteRemoved',
  'notesFailed'
]);
```

In this example `notes` module has couple of private actions, but second argument allows expose them.

### Redux async actions

In tests for async actions we are expect for other actions to be executed.
And here is 'to dispatch actions' behaviour for expect method.

```es6
it('fetchNotes', function *() {
  const expectedActions = [
    { type: 'NOTES_RECEIVE', payload: [entity, notesPayload]}
  ];

  yield expect(actions.fetchNotes(entity), 'to dispatch actions', expectedActions);
});
```

For expected actions entry you can use

* `{ type: 'STRING_TYPE', ...otherPropsToSatisfy }`
* `{ type: importedActions.someAction, ...otherPropsToSatisfy }`
* `'STRING_TYPE'`
* `importedActions.someAction`

For example.

```es6
it('createNote, editNote', function *() {
  const expectedActions = [
    actions.stopAddingOrEditingNote,
    { type: actions.updateNotes, payload: [entity, [notePayload]]}
  ];

  yield expect(actions.createNote(entity, notePayload), 'to dispatch actions', expectedActions);
});
```

For mocking api requests use [nock](https://www.npmjs.com/package/nock#read-this-1-about-interceptors)

```es6
it('editNote', function *() {
  nock(process.env.API_URL)
    .patch(notesUri(entity, 1))
    .reply(200, notePayload);

  yield expect(actions.editNote(entity, 1, notePayload), 'to dispatch actions', expectedActions);
});
```

Important thing for how nock working:

> When you setup an interceptor for an URL and that interceptor is used, it is removed from the interceptor list.
> This means that you can intercept 2 or more calls to the same URL and return different things on each of them.
> It also means that you must setup one interceptor for each request you are going to have,
> otherwise nock will throw an error because that URL was not present in the interceptor list.

### Redux reducers

Tests for reducers are very simple.

```es6
const newState = reducer(state, actions.updateNotes(entity, [notePayload]));

// do any assertions on newState
```

Perhaps [`to satisfy`](http://unexpected.js.org/assertions/any/to-satisfy/) and
[`to have properties`](http://unexpected.js.org/assertions/object/to-have-properties/) methods may be helpful for asserting state structure.


#### Test coverage for reducers

I think only complex logic in reducers should be tested in common cases.
For example if we have reducer like that:

```es6
// on some action:
return dissoc(state, [entityType, entityId, 'editingNoteId']);
```

This construction is already declarative and safely.
Of course if you very love tests and writing tests before code, you can write tests for anything you want :)

## Glossary

* [Writing Tests](http://redux.js.org/docs/recipes/WritingTests.html) article on redux.js.org

## npm packages

* [nock](https://www.npmjs.com/package/nock#read-this-1-about-interceptors)
* [unexpected](http://unexpected.js.org)
* [unexpected-react-shallow](https://github.com/bruderstein/unexpected-react-shallow#assertions)
* [ShallowTestUtils](https://github.com/sheepsteak/react-shallow-testutils#react-shallow-testutils)
* [ReactTestUtils](https://facebook.github.io/react/docs/test-utils.html)
