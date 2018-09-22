resolvers := Seq(
  "Artifactory klarrio" at "https://klarrio.jfrog.io/klarrio/jvm-libs/"
)
externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)
credentials += Credentials(Path.userHome / ".ivy2" / ".klarrio-credentials")

addSbtPlugin("com.klarrio" % "klarrio-sbt-plugin" % "1.0.10")