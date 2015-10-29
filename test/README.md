# Writing Tests

We have two types of tests, for now.

* Acceptance tests, for testing components
* Unit tests, for testing redux modules and other stuff

## Components

There are several approaches for testing React components,
which depend on how component declared and how component rendered.

Accordingly, there are several npm packages for testing components
rendered in DOM and rendering just in some internal tree representation of component markup.

[ShallowTestUtils](https://github.com/sheepsteak/react-shallow-testutils#react-shallow-testutils),
support library for testing shallow rendered components.

`import ShallowTestUtils from 'react-shallow-testutils';`

[ReactTestUtils](https://facebook.github.io/react/docs/test-utils.html),
support library for testing rendered components in DOM.

`import TestUtils from 'react-addons-test-utils';`

### Shallow render components

Stateless components are rendered to tree structure via calling them to defined props.
And you can test what happened via looking/asserting to that tree structure.

Example:

    const button = PrimaryButton({icon: 'add'});
    expect(button.type).to.equal('SimpleButton');
    expect(button.props.className).to.contain('fc-btn-primary');
    expect(button.props.children[0].props.className).to.contain('icon-add')

Also you can shallow render common component via global function `shallowRender`:

    const contentBox = shallowRender(
      <ContentBox title={ title } className="" />
    );

You can call instance methods via `instance` property:

    contentBox.instance.tick();
    expect(contentBox.props.className).to.equal('fc-content-box');

In both cases you can use [`ShallowTestUtils`](https://github.com/sheepsteak/react-shallow-testutils#react-shallow-testutils)
for retrieve some elements based on defined criteria.

    ShallowTestUtils.findWithClass(dialog, 'fc-modal');

Tip: `ShallowTestUtils.findWithClass` and other methods that select only single element always raise exception if
element not found, so you don't need for constructions like `except(findWithClass(...)).to.be.ok`.

### Render components in DOM

For render commmon components to DOM use `yield renderIntoDocument(jsx, shouldAppendToDocument)` construction.

    const formfield = yield renderIntoDocument(
      <FormField maxLength={5} validator='ascii' label="Lorem Ipsum">
        <input type="text" value="Кошку ела собака"/>
      </FormField>,
      true
    );

If second parameter is `true` render dom will be appended to DOM,
so you can get some elements via calling `document.getElementById(...)` for example.

In this case you can use `TestUtils` for retrieving rendered elements from rendered component, e.g.:

    const inputNode = TestUtils.findRenderedDOMComponentWithTag(formfield, 'input');

Tip: `TestUtils.findRenderedDOMComponentWithTag` already returns dom element if you are looking for native element.
I.e. you don't need to call `findDOMNode` to `findRenderedDOMComponentWithTag` result.

### Render stateless components in DOM

For render stateless components to DOM use same approch for render common components, but there is one exception -
you should always wrap stateless component into `<div>`:

    const { container } = yield renderIntoDocument(
      <div><LineItems {...defaultProps} /></div>
    );

Also you can't use TestUtils or ShallowTestUtils in this case, so, use native DOM methods for that, `querySelector` for example.

    expect(container.querySelector('.fc-modal')).to.not.equal(null);


## Glossary

* [Writing Tests](http://redux.js.org/docs/recipes/WritingTests.html) article on redux.js.org
