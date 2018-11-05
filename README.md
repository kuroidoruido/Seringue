# Seringue

Seringue is a Static Dependency Injector with no dependencies.

## Features

- Easy to use and understand
- Does not add any runtime dependency
- Dependencies and injection is computed compiled-time
- The Seringue JARs depends only of Java Standard Platform and have no dependencies

## How to use Seringue with Maven?

### Add compile scope dependency to annotation

```
...
<dependencies>
	...
	<dependency>
		<groupId>nf.fr.k49.seringue</groupId>
		<artifactId>seringue-annotations</artifactId>
		<version>0.1.0</version>
		<scope>compile</scope>
	</dependency>
	...
</dependencies>
...
```

This dependency is only needed compile time and will not be use at runtime.

### Add the Seringue compiler to your maven build step
```
...
<build>
	<plugins>
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-compiler-plugin</artifactId>
			<version>3.8.0</version>
			<configuration>
				<showWarnings>true</showWarnings>
				<annotationProcessorPaths>
					<path>
						<groupId>nf.fr.k49.seringue</groupId>
						<artifactId>seringue-compiler</artifactId>
						<version>0.1.0</version>
					</path>
				</annotationProcessorPaths>
				<annotationProcessors>
					<annotationProcessor>nf.fr.k49.seringue.compiler.InjectionCompiler</annotationProcessor>
				</annotationProcessors>
			</configuration>
		</plugin>
	</plugins>
</build>
...
```

### Add @Singleton annotation

The @Singleton annotation must be placed on constructors.

Any constructor with @Singleton will be used as type constructor and all parameters will be expected to be other @Singleton types.

Example:

```
class A {
	private B b;
	@Singleton
	public A(B b) {
		this.b = b;
	}
}

class B {
	@Singleton
	public B() {
	}
}
```

See more usage example here: https://github.com/kuroidoruido/Seringue/tree/master/seringue-demo-app/
