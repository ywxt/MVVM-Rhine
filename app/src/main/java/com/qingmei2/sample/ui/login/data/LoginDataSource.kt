package com.qingmei2.sample.ui.login.data

import com.qingmei2.rhine.base.repository.RhineRepositoryBoth
import com.qingmei2.sample.db.LoginEntity
import com.qingmei2.sample.db.UserDatabase
import com.qingmei2.sample.http.RxSchedulers
import com.qingmei2.sample.http.entity.LoginUser
import com.qingmei2.sample.http.service.ServiceManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class LoginDataSourceRepository(
        remoteDataSource: ILoginRemoteDataSource,
        localDataSource: ILoginLocalDataSource
) : RhineRepositoryBoth<ILoginRemoteDataSource, ILoginLocalDataSource>(remoteDataSource, localDataSource) {

    fun login(username: String, password: String): Single<LoginUser> =
            remoteDataSource.login(username, password)
                    .flatMap {
                        localDataSource.insertUser(it)  // save user
                                .andThen(Single.just(it))
                    }
}

class LoginRemoteDataSource(
        private val serviceManager: ServiceManager
) : ILoginRemoteDataSource {

    override fun login(username: String, password: String): Single<LoginUser> =
            serviceManager.loginService
                    .login(username, password)
                    .subscribeOn(RxSchedulers.io)

}

class LoginLocalDataSource(
        private val database: UserDatabase
) : ILoginLocalDataSource {

    override fun insertUser(user: LoginUser): Completable =
            Completable.fromAction {
                database.loginDao()
                        .insert(user = LoginEntity(user.id, user.login, "", user.avatarUrl))
            }.subscribeOn(RxSchedulers.database)

    override fun findUserByUsername(username: String): Flowable<LoginEntity> =
            database.loginDao()
                    .findUserByName(username)
                    .subscribeOn(RxSchedulers.database)
}