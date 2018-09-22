//protobuf plugins (keep in this position, before all other plugins, otherwise it might crash is mentioned in this issue https://github.com/scalapb/ScalaPB/issues/150#issuecomment-262892788)
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.18")
libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.7"

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.2.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

