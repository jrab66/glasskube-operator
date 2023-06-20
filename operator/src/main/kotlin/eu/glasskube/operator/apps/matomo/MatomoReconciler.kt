package eu.glasskube.operator.apps.matomo

import eu.glasskube.kubernetes.client.patchOrUpdateStatus
import eu.glasskube.operator.api.reconciler.informerEventSource
import eu.glasskube.operator.apps.matomo.dependent.MatomoConfigMap
import eu.glasskube.operator.apps.matomo.dependent.MatomoConfigSecret
import eu.glasskube.operator.apps.matomo.dependent.MatomoDatabaseSecret
import eu.glasskube.operator.apps.matomo.dependent.MatomoDeployment
import eu.glasskube.operator.apps.matomo.dependent.MatomoIngress
import eu.glasskube.operator.apps.matomo.dependent.MatomoService
import eu.glasskube.operator.apps.matomo.dependent.mariadb.MatomoDatabaseMariaDB
import eu.glasskube.operator.apps.matomo.dependent.mariadb.MatomoGrantMariaDB
import eu.glasskube.operator.apps.matomo.dependent.mariadb.MatomoMariaDB
import eu.glasskube.operator.apps.matomo.dependent.mariadb.MatomoUserMariaDB
import io.fabric8.kubernetes.api.model.Secret
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent

@ControllerConfiguration(
    dependents = [
        Dependent(type = MatomoDeployment::class, dependsOn = ["MatomoMariaDB"]),
        Dependent(
            type = MatomoDatabaseSecret::class,
            useEventSourceWithName = MatomoReconciler.SECRET_EVENT_SOURCE
        ),
        Dependent(
            type = MatomoConfigSecret::class,
            useEventSourceWithName = MatomoReconciler.SECRET_EVENT_SOURCE
        ),
        Dependent(type = MatomoConfigMap::class),
        Dependent(type = MatomoService::class),
        Dependent(type = MatomoIngress::class),
        Dependent(
            type = MatomoMariaDB::class,
            name = "MatomoMariaDB",
            readyPostcondition = MatomoMariaDB.ReadyPostCondition::class
        ),
        Dependent(type = MatomoDatabaseMariaDB::class),
        Dependent(type = MatomoUserMariaDB::class),
        Dependent(type = MatomoGrantMariaDB::class)
    ]
)
class MatomoReconciler : Reconciler<Matomo>, EventSourceInitializer<Matomo> {
    override fun reconcile(resource: Matomo, context: Context<Matomo>): UpdateControl<Matomo> {
        return resource.patchOrUpdateStatus(MatomoStatus())
    }

    override fun prepareEventSources(context: EventSourceContext<Matomo>) = with(context) {
        mapOf(SECRET_EVENT_SOURCE to informerEventSource<Secret>())
    }

    companion object {
        const val LABEL = "glasskube.eu/Matomo"
        const val APP_NAME = "matomo"
        const val SELECTOR = "app.kubernetes.io/managed-by=glasskube-operator,app=$APP_NAME"
        internal const val SECRET_EVENT_SOURCE = "MatomoSecretEventSource"
    }
}
