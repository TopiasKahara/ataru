# lomake-editori

## Development Mode

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

```
lein less auto
```

### Run application:

```
lein clean
lein with-profile figwheel-standalone figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Preferred way to run application

```
lein with-profile dev run
(in another terminal)
lein with-profile dev figwheel dev
```

Browse to [http://localhost:3450](http://localhost:3450).

### Run tests:

[doo](https://github.com/bensu/doo) assumes that [PhantomJS](http://phantomjs.org/) is installed locally to `./node_modules/phantomjs-prebuilt/bin/phantomjs`. The `package.json` in this repository does just that:

```
npm install
```

To actually run tests:

```
lein clean
lein doo phantom test once
```

### Clojure linter

```
lein eastwood
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn). 

## Production Build

```
lein clean
lein cljsbuild once min
```
