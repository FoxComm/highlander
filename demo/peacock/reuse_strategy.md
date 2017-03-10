## Goal

It would be ideal to have some platform, e.g. Storefront SDK, that offers a high level of code reusability, but at the same has the opportunity to customize the content whichever way you want. The task is not an easy one, but it's definitely doable.

Later it can be a great foundation for building our own CMS, where the platform can be used by anyone without prior programming knowledge, i.e. drag-and-drop.


## The Way of Developing

It is necessary to move towards writing modular code (compared to the current monolithic style), considering that our end goal is having reusable blocks of code. For example, we should have separate modules for checkout, billing forms, etc.
Also, it must have strictly defined user interfaces. For this job it's better to use TypeScript than ES6 + flow.

Ideally, we've got the following:

Skeleton -> Modules(Components) tree

From React point of view, a module is represented by React Component, but has a strictly defined user interface and includes:

1. Logic for receiving and processing data from API
2. Logic for routing the module
3. Static resources (images, fonts, etc.)
4. Any other meta information crucial for the impeccable system's operation

This approach will help to get rid of the monolithic construction and get the modular one instead.

For sustaining the modularity, the app must have:

1. A way to connect extra routes from the module
2. An ability to track the interconnection between modules, and react accordingly
3. A way to connect static resources from the modules

## Reusability Mechanism

To start off, let's look at the component's lifecycle.

Component:

1. Gets the data from API and processes it
2. Renders the data
3. Has the necessary logic for dealing with user and system events  
4. Talks to the API and the system

Of course, any of the aforementioned pieces may need a customization for the final product.
But in most cases, only the frontend must be changed.

Unfortunately, in React the number 2, 3, and 4 on the list are closely intertwined. But we can try to minimize this connection with the tools available out there.

Actually, the best score for reusablity at the data representation level belongs to XSLT technology and the like, e.g. https://github.com/pasaran/yate with xml-free syntax.
That's a truly powerful instrument which possesses a great ability to transform templates into what you need.

So on the module level we also have a strategy of code modularity.

What we already can do right now.

First of all, divide components into two groups: smart containers and dumb presenters, both of which have a defined user interface.
All the logic for API handling, processing the data, and interacting with the system resides in smart containers.

That should be enough for the first stage, considering, for instance, an inheritance mechanism, in which we can predefine the smart container's methods, or change the presenter component altogether.  

## Modules' Inheritance Mechanism
TBD
